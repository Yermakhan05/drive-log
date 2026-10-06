from django.db import models


class Trip(models.Model):
    PAYMENT_CASH = "cash"
    PAYMENT_CARD = "card"
    PAYMENT_CHOICES = [
        (PAYMENT_CASH, "Cash"),
        (PAYMENT_CARD, "Card"),
    ]

    id = models.CharField(primary_key=True, max_length=64)
    start = models.DateTimeField()
    end = models.DateTimeField()
    amount = models.DecimalField(max_digits=12, decimal_places=2)
    payment = models.CharField(max_length=4, choices=PAYMENT_CHOICES)
    commission = models.DecimalField(max_digits=12, decimal_places=2)

    class Meta:
        constraints = [
            models.CheckConstraint(
                condition=models.Q(amount__gt=0),
                name="trip_amount_gt_0",
            ),
            models.CheckConstraint(
                condition=models.Q(commission__gte=0),
                name="trip_commission_gte_0",
            ),
            models.CheckConstraint(
                condition=models.Q(payment__in=["cash", "card"]),
                name="trip_payment_cash_or_card",
            ),
            models.CheckConstraint(
                condition=models.Q(end__gt=models.F("start")),
                name="trip_end_after_start",
            ),
        ]

    def __str__(self) -> str:
        return self.id
