import json
import boto3
from opentelemetry import trace
from opentelemetry import propagate
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator

# def MessageCarrierSetter():
#     return


def lambda_handler(event, context):
    tracer = trace.get_tracer(__name__)
    notification = "Here is the SNS notification for Lambda function tutorial."

    with tracer.start_as_current_span("Sending Message to SNS", kind=trace.SpanKind.PRODUCER) as span:

        carrier = {}
        TraceContextTextMapPropagator().inject(carrier)

        print(str(span.get_span_context()))

        client = boto3.client("sns")

        print(json.dumps(carrier))

        target_sns_arn = os.getenv('SNS_ARN')
        response = client.publish(
            TargetArn=target_sbs_arn,
            Message=json.dumps({"default": notification}),
            MessageAttributes={"carrier": {"DataType": "String", "StringValue": json.dumps(carrier)}},
            MessageStructure="json",
        )

    return {
        "statusCode": 200,
        "body": json.dumps(response),
    }
