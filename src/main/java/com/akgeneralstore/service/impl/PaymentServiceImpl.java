package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.PaymentRequest;
import com.akgeneralstore.dto.request.RazorpayOrderRequest;
import com.akgeneralstore.dto.request.RazorpayVerifyRequest;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.dto.response.PaymentConfigResponse;
import com.akgeneralstore.dto.response.RazorpayOrderResponse;
import com.akgeneralstore.entity.Order;
import com.akgeneralstore.entity.Payment;
import com.akgeneralstore.entity.StoreSetting;
import com.akgeneralstore.enums.CollectionMethod;
import com.akgeneralstore.enums.PaymentMode;
import com.akgeneralstore.enums.PaymentStatus;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.OrderRepository;
import com.akgeneralstore.repository.PaymentRepository;
import com.akgeneralstore.repository.StoreSettingRepository;
import com.akgeneralstore.service.OrderService;
import com.akgeneralstore.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String RAZORPAY_CURRENCY = "INR";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final StoreSettingRepository storeSettingRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${payment.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${payment.business.name:AK General Store}")
    private String businessName;

    @Value("${payment.business.logo:https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80}")
    private String businessLogo;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            StoreSettingRepository storeSettingRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.storeSettingRepository = storeSettingRepository;
    }

    @Override
    public OrderResponse createPayment(PaymentRequest request, Long userId) {
        Order order = getOwnedOrder(request.getOrderId(), userId);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(Payment::new);
        payment.setOrderId(order.getId());
        payment.setAmount(request.getAmount() == null ? order.getTotalAmount() : request.getAmount());
        payment.setMode(request.getPaymentMode());
        payment.setTransactionId(
                request.getReferenceId() == null ? "MANUAL-" + System.currentTimeMillis() : request.getReferenceId()
        );
        payment.setStatus(request.getPaymentMode() == PaymentMode.UPI ? PaymentStatus.SUCCESS : PaymentStatus.PENDING);
        payment.setCollectionMethod(request.getPaymentMode() == PaymentMode.UPI ? CollectionMethod.UPI : null);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (request.getPaymentMode() == PaymentMode.UPI) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            orderRepository.save(order);
        }

        return orderService.getUserOrders(userId).stream()
                .filter(item -> item.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    public PaymentConfigResponse getPaymentConfig() {
        return PaymentConfigResponse.builder()
                .razorpayEnabled(razorpayEnabled && hasRazorpayKeys())
                .razorpayKeyId(hasRazorpayKeys() ? razorpayKeyId : "")
                .businessName(readSetting("store_name", businessName))
                .businessLogo(businessLogo)
                .upiMerchantName(readSetting("upi_merchant_name", "AK General Store"))
                .upiId(readSetting("upi_id", ""))
                .deliveryBasePayoutAmount(readSetting("delivery_base_payout_amount", "20"))
                .deliveryAdditionalPayoutAmount(readSetting("delivery_additional_payout_amount", "10"))
                .build();
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request, Long userId) {
        if (!(razorpayEnabled && hasRazorpayKeys())) {
            throw new BadRequestException("Razorpay is not configured on the server.");
        }

        Order order = getOwnedOrder(request.getOrderId(), userId);
        if (order.getPaymentMode() != PaymentMode.RAZORPAY) {
            throw new BadRequestException("This order is not marked for Razorpay payment.");
        }

        try {
            long amountInSubunits = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();
            String payload = String.format(
                    "{\"amount\":%d,\"currency\":\"%s\",\"receipt\":\"%s\",\"notes\":{\"order_number\":\"%s\"}}",
                    amountInSubunits,
                    RAZORPAY_CURRENCY,
                    order.getOrderNumber(),
                    order.getOrderNumber()
            );

            String authValue = Base64.getEncoder()
                    .encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/orders"))
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Razorpay order creation failed: " + response.body());
            }

            String razorpayOrderId = extractJsonValue(response.body(), "id");
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(Payment::new);
            payment.setOrderId(order.getId());
            payment.setAmount(order.getTotalAmount());
            payment.setMode(PaymentMode.RAZORPAY);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTransactionId("RZP-ORDER-" + order.getOrderNumber());
            payment.setProviderOrderId(razorpayOrderId);
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            return RazorpayOrderResponse.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amountInSubunits)
                    .currency(RAZORPAY_CURRENCY)
                    .keyId(razorpayKeyId)
                    .businessName(businessName)
                    .businessLogo(businessLogo)
                    .build();
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Unable to create Razorpay order: " + exception.getMessage());
        }
    }

    @Override
    public OrderResponse verifyRazorpayPayment(RazorpayVerifyRequest request, Long userId) {
        if (!(razorpayEnabled && hasRazorpayKeys())) {
            throw new BadRequestException("Razorpay is not configured on the server.");
        }

        Order order = getOwnedOrder(request.getOrderId(), userId);
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));

        if (!request.getRazorpayOrderId().equals(payment.getProviderOrderId())) {
            throw new BadRequestException("Razorpay order mismatch.");
        }

        String generatedSignature = generateSignature(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayKeySecret
        );

        if (!generatedSignature.equals(request.getRazorpaySignature())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProviderPaymentId(request.getRazorpayPaymentId());
            payment.setProviderSignature(request.getRazorpaySignature());
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new BadRequestException("Payment signature verification failed.");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setProviderPaymentId(request.getRazorpayPaymentId());
        payment.setProviderSignature(request.getRazorpaySignature());
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order);

        return orderService.getUserOrders(userId).stream()
                .filter(item -> item.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    public OrderResponse markPaymentFailed(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseGet(Payment::new);
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotalAmount());
        payment.setMode(order.getPaymentMode());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setTransactionId("FAILED-" + order.getOrderNumber());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);

        return orderService.getUserOrders(userId).stream()
                .filter(item -> item.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private Order getOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("This order does not belong to the logged-in user.");
        }

        return order;
    }

    private boolean hasRazorpayKeys() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
                && razorpayKeySecret != null && !razorpayKeySecret.isBlank();
    }

    private String readSetting(String key, String fallback) {
        return storeSettingRepository.findBySettingKey(key)
                .map(StoreSetting::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(fallback);
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) {
            return "";
        }

        int valueStart = start + search.length();
        int valueEnd = json.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            return "";
        }

        return json.substring(valueStart, valueEnd);
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BadRequestException("Unable to verify Razorpay signature.");
        }
    }
}
