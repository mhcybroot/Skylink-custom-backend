package root.cyb.mh.attendancesystem.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxSizeException(MaxUploadSizeExceededException exc,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String uri = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");

        if (uri != null && (uri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains("application/json")))) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "File upload limit exceeded. Maximum request limit is 200MB.");
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
        }

        redirectAttributes.addFlashAttribute("errorMessage", "File too large! Maximum upload size exceeded.");

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }

        return "redirect:/payment-requests";
    }
}
