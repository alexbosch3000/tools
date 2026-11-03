// Turns domain exceptions and database constraint failures into RFC 9457 problem responses.
package io.github.alexbosch3000.familyhub.shared.web;

import io.github.alexbosch3000.familyhub.shared.ConflictException;
import io.github.alexbosch3000.familyhub.shared.InvalidRequestException;
import io.github.alexbosch3000.familyhub.shared.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalid(InvalidRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail conflict(ConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** A foreign key or unique constraint failure means the data is used elsewhere or duplicated. */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail dataAccess(DataAccessException e) {
        if (e.getMostSpecificCause() instanceof SQLiteException sqlite && isConstraint(sqlite)) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "Opération impossible : l'élément est utilisé ailleurs ou existe déjà.");
        }
        LOG.error("Database error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de base de données.");
    }

    private static boolean isConstraint(SQLiteException e) {
        SQLiteErrorCode code = e.getResultCode();
        return code != null && code.name().startsWith("SQLITE_CONSTRAINT");
    }
}
