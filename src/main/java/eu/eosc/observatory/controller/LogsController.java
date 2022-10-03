package eu.eosc.observatory.controller;

import org.apache.logging.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;

public class LogsController extends gr.athenarc.catalogue.controller.LogsController {

    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);

    @Override
//    @PostMapping("level/root")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setRootLogLevel(@RequestParam Level standardLevel) {
        return super.setRootLogLevel(standardLevel);
    }

    @Override
//    @PostMapping("level/package/{path}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setPackageLogLevel(@PathVariable("path") String path, @RequestParam Level standardLevel) {
        return super.setPackageLogLevel(path, standardLevel);
    }

    @Override
//    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> fetchLogFile(@RequestParam(required = false) String filepath, HttpServletResponse response) {
        return super.fetchLogFile(filepath, response);
    }

}
