import json
from pathlib import Path

from django.core.management.base import BaseCommand, CommandError
from rest_framework.exceptions import ValidationError

from trips.services import DuplicateTripError, create_trip


class Command(BaseCommand):
    help = "Import trips from a JSON array using the same validation as the API."

    def add_arguments(self, parser) -> None:
        parser.add_argument(
            "json_path",
            type=str,
            help="Path to a JSON file containing an array of trips.",
        )

    def handle(self, *args, **options) -> None:
        json_path = Path(options["json_path"])
        if not json_path.is_file():
            raise CommandError(f"File not found: {json_path}")

        try:
            payload = json.loads(json_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as exc:
            raise CommandError(f"Invalid JSON: {exc}") from exc

        if not isinstance(payload, list):
            raise CommandError("JSON file must contain an array of trips.")

        imported = 0
        skipped = 0
        for item in payload:
            try:
                create_trip(item)
            except (DuplicateTripError, ValidationError, TypeError):
                skipped += 1
            else:
                imported += 1

        self.stdout.write(f"Imported: {imported}")
        self.stdout.write(f"Skipped: {skipped}")
