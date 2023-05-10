package com.athar.orderservice.service;

import com.athar.orderservice.dto.InventoryResponse;
import com.athar.orderservice.dto.OrderListItemsDto;
import com.athar.orderservice.dto.OrderRequest;
import com.athar.orderservice.event.OrderPlacedEvent;
import com.athar.orderservice.model.Order;
import com.athar.orderservice.model.OrderLineItems;
import com.athar.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class  OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderListItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();

        // Create a span. If there was a span present in this thread it will become
        // the `newSpan`'s parent.
        Span newSpan = this.tracer.nextSpan().name("calculateTax");

        // Start a span and put it in scope. Putting in scope means putting the span
        // in thread local
        // and, if configured, adjust the MDC to contain tracing information
        try (Tracer.SpanInScope ws = this.tracer.withSpan(newSpan.start())) {

            // call inventory service, and place order if product is in stock
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsIsInStock = Arrays.stream(inventoryResponseArray)
                    .allMatch(InventoryResponse::isInStock);

            if(allProductsIsInStock) {
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
                return "Order placed successfully!";

            }else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

    } finally {

            // Once done remember to end the span. This will allow collecting
            // the span to send it to a distributed tracing system e.g. Zipkin
            newSpan.end();
        }
    }

    private OrderLineItems mapToDto(OrderListItemsDto orderListItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderListItemsDto.getPrice());
        orderLineItems.setQuantity(orderListItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderListItemsDto.getSkuCode());
        return orderLineItems;
    }

}
