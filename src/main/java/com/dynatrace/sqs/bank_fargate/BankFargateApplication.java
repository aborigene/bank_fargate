package com.dynatrace.sqs.bank_fargate;

import java.util.List;

import org.json.JSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@SpringBootApplication
public class BankFargateApplication {

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

    Tracer tracer = openTelemetry.getTracer("dynatrace-manual-opentel", "1.0.0");

    public static void main(String[] args) {
        SpringApplication.run(BankFargateApplication.class, args);
    }

    public class StringToHexadecimal {

        String getValue(String integer) {
            StringBuffer sb = new StringBuffer();
            // Converting string to character array
            char ch[] = integer.toCharArray();
            // System.out.println("Converted: "+
            // Long.toHexString(Integer.parseInt(integer)));
            System.out.println("Converted: " + String.format("%x", integer));
            for (int i = 0; i < ch.length; i++) {
                String hexString = Integer.toHexString(ch[i]);

                System.out.println("ch[i]: " + ch[i]);
                System.out.println("hexString: " + hexString);
                sb.append(hexString);
            }
            System.out.println("Hex Conversion: " + sb.toString());
            return (sb.toString());
        }
    }

    TextMapGetter<JSONObject> getter = new TextMapGetter<>() {
        @Override
        public String get(JSONObject carrier, String key) {
            System.out.println("TextMapGetter - " + key);

            JSONObject carrier_data = (JSONObject) carrier.get("carrier");
            String value = carrier_data.get("Value").toString();
            JSONObject propagationValue = new JSONObject(value);
            String traceparent = propagationValue.get("traceparent").toString();

            if (traceparent != null) {
                return traceparent;
            }
            return null;
        }

        @Override
        public Iterable<String> keys(JSONObject carrier) {
            return null;
        }

    };

    public void processMessage(ReceiveMessageRequest receiveRequest, SqsClient sqsClient) throws Exception {
        List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

        // System.out.println(messages);

        // Print out the messages
        if (messages.size() == 0) {
            // throw new Exception("Sem mensagens na fila");
            return;
        }
        for (Message m : messages) {
            // System.out.println("\n" +m.body());

            try {
                JSONObject messageBody = new JSONObject(m.body());
                JSONObject mattrib = (JSONObject) messageBody.get("MessageAttributes");
                TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
                Context parentContext = propagator.extract(Context.current(), mattrib, getter);
                Span span = tracer.spanBuilder("process message")
                        .setParent(parentContext)
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan();

                System.out.println("parentContext: " + parentContext);

                try {
                    // Add the attributes defined in the Semantic Conventions
                    span.setAttribute("key", "value");

                } finally {
                    span.end();
                }

            } catch (NullPointerException e) {
                System.out.println("Encontrado valor nulo.");
            }
        }

    }

    @Bean
    public CommandLineRunner commandLineRunnerX(ApplicationContext ctx) {
        return args -> {
            System.out.println("no span:" + Context.current());

            SqsClient sqsClient = SqsClient.builder()
                    .region(Region.US_EAST_1)
                    .build();
            System.out.println("Built client");
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(System.getenv("QUEUE_URL"))
                        .build();
                System.out.println("Built receive request");

                while (true) {
                    try {
                        System.out.println("my span:" + Context.current());
                        // put the span into the current Context
                        processMessage(receiveRequest, sqsClient);
                        Thread.sleep(100);
                    } catch (Exception err) {
                        System.out.println("Exceção: " + err.getMessage());
                        err.printStackTrace();
                    } finally {
                        System.out.println("terminou...");
                    }
                }

            } catch (QueueNameExistsException e) {
                throw e;
            }

        };
    }

}
