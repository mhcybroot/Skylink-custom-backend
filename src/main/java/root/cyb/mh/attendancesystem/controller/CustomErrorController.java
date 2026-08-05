package root.cyb.mh.attendancesystem.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int statusCode = 500;
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
                if (statusCode == 200) {
                    statusCode = 500;
                }
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("status", statusCode);
        model.addAttribute("error", message != null && !message.toString().isBlank() ? message.toString() : "An unexpected error occurred.");
        return "error";
    }
}
