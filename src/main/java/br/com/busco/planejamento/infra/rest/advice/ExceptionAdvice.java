package br.com.busco.planejamento.infra.rest.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice(annotations = RestController.class)
public class ExceptionAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handler(Exception exception) {
        exception.printStackTrace();
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ExceptionResponse.from(exception));
    }

//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ConstraintViolationResponse> handler(ConstraintViolationException exception) {
//
//        ConstraintViolationResponseBuilder builder = ConstraintViolationResponse.builder();
//
//        builder.operation(ExceptionResponse.from(exception));
//
//        builder.details(exception.getConstraintViolations().stream()
//                .map(ExceptionResponse::from).toList());
//        // .forEach(r -> builder.details);
//
//        return ResponseEntity.status(BAD_REQUEST)
//                .body(builder.build());
//    }

}
