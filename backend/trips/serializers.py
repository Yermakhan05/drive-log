from decimal import Decimal

from rest_framework import serializers

from trips.models import Trip


class DateQuerySerializer(serializers.Serializer):
    date = serializers.DateField(
        required=True,
        input_formats=["%Y-%m-%d"],
        error_messages={
            "required": "Date is required.",
            "invalid": "Date must be in YYYY-MM-DD format.",
            "null": "Date is required.",
        },
    )


class MoneyNumberField(serializers.Field):
    def to_representation(self, value):
        amount = Decimal(value or 0)
        if amount == amount.to_integral_value():
            return int(amount)
        return float(amount)


class DailySummarySerializer(serializers.Serializer):
    date = serializers.DateField()
    trips_count = serializers.IntegerField()
    revenue = MoneyNumberField()
    commission = MoneyNumberField()
    net_income = MoneyNumberField()
    cash = MoneyNumberField()
    card = MoneyNumberField()


class TripSerializer(serializers.ModelSerializer):
    class Meta:
        model = Trip
        fields = ("id", "start", "end", "amount", "payment", "commission")
        extra_kwargs = {
            "id": {"validators": []},
        }

    def validate_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("Amount must be greater than 0.")
        return value

    def validate_commission(self, value):
        if value < 0:
            raise serializers.ValidationError(
                "Commission must be greater than or equal to 0."
            )
        return value

    def validate_payment(self, value):
        allowed = {Trip.PAYMENT_CASH, Trip.PAYMENT_CARD}
        if value not in allowed:
            raise serializers.ValidationError("Payment must be either 'cash' or 'card'.")
        return value

    def validate(self, attrs):
        start = attrs.get("start", getattr(self.instance, "start", None))
        end = attrs.get("end", getattr(self.instance, "end", None))
        if start is not None and end is not None and end <= start:
            raise serializers.ValidationError(
                {"end": "End must be later than start."}
            )
        return attrs
