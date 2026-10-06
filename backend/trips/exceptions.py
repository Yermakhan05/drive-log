from rest_framework import status
from rest_framework.exceptions import APIException
from rest_framework.views import exception_handler

from trips.services import DuplicateTripError


class DuplicateTripConflict(APIException):
    status_code = status.HTTP_409_CONFLICT
    default_detail = "A trip with this id already exists."
    default_code = "conflict"


def api_exception_handler(exc, context):
    if isinstance(exc, DuplicateTripError):
        exc = DuplicateTripConflict({"id": [str(exc)]})

    response = exception_handler(exc, context)
    if response is None:
        return None

    data = response.data
    if isinstance(data, dict) and "errors" in data:
        return response

    if isinstance(data, dict) and "detail" in data and len(data) == 1:
        response.data = {
            "errors": {
                "non_field_errors": [str(data["detail"])],
            }
        }
        return response

    response.data = {"errors": data}
    return response
