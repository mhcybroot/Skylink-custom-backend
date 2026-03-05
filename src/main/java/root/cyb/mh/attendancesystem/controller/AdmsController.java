package root.cyb.mh.attendancesystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import root.cyb.mh.attendancesystem.service.AdmsService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/iclock")
public class AdmsController {

    @Autowired
    private AdmsService admsService;

    // Handshake
    @GetMapping("/cdata")
    public String handshake(HttpServletRequest request) {
        return "OK";
    }

    // Data Push
    @PostMapping("/cdata")
    public String receiveData(@RequestParam(required = false) String SN,
            @RequestParam(required = false) String table,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        // Body might be null/empty, just pass safety empty string
        return admsService.processCdata(SN, table, body != null ? body : "");
    }

    // Command Request (Device asks "Any commands for me?")
    @GetMapping("/getrequest")
    public String getRequest(@RequestParam String SN) {
        return admsService.getPendingCommand();
    }

    // Registry check
    @GetMapping("/registry")
    public String registry(@RequestParam String SN) {
        return "RegistryCode=QWC5251100143"; // Or whatever logic
    }

    @PostMapping("/fdata")
    public String receiveFdata(@RequestParam(required = false) String SN,
            @RequestParam(required = false) String table,
            @RequestBody(required = false) String body,
            HttpServletRequest request) {
        System.out.println("ADMS CHECK: Received fdata POST from SN: " + SN + " Table: " + table);
        // We can pass this to the parser as well, but for now just returning "OK"
        // clears the device queue
        if (body != null && table != null) {
            admsService.processCdata(SN, table, body);
        }
        return "OK";
    }

    // Handle command results
    @PostMapping("/devicecmd")
    public String receiveCmdResult(@RequestParam(required = false) String SN,
            @RequestBody(required = false) String body) {
        System.out.println("ADMS CHECK: Received command result from SN: " + SN + ": " + body);
        return "OK";
    }
}
