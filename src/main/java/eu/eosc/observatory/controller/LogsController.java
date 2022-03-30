package eu.eosc.observatory.controller;

import eu.openminted.registry.core.exception.ServerError;
import gr.athenarc.catalogue.exception.ResourceException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("logs")
public class LogsController {

    private static final Logger logger = LogManager.getLogger(LogsController.class);

    @PostMapping("level/root")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setRootLogLevel(@RequestParam Level standardLevel) {
        logger.info("Changing Root Level Logging to: " + standardLevel);
        Configurator.setRootLevel(standardLevel);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("level/package/{path}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> setPackageLogLevel(@PathVariable("path") String path, @RequestParam Level standardLevel) {
        logger.info(String.format("Changing '%s' Logger Level to '%s'", path, standardLevel));
        Configurator.setLevel(path, standardLevel);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> fetchLogFile(@RequestParam(required = false) String filepath, HttpServletResponse response) {
        String logsDir = "logs/";

        if (filepath == null || "".equals(filepath.trim())) {
            return ResponseEntity.ok().body(getFolderContents(logsDir));
        } else {
            ByteArrayResource resource = readFile(filepath);
            response.setHeader("Content-disposition", "attachment; filename=" + Paths.get(filepath).getFileName().toString());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
        }
    }

    private ByteArrayResource readFile(String filepath) {
        ByteArrayResource resource = null;

        try {
            Path path = Paths.get(filepath);
            resource = new ByteArrayResource(Files.readAllBytes(path));
        } catch (Exception e) {
            logger.error(e);
            throw new ResourceException("File '" + filepath + "' does not exist..", HttpStatus.NOT_FOUND);
        }

        return resource;
    }

    private List<String> getFolderContents(String dir) {
        List<String> folderContents = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(dir))) {
            folderContents = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e);
        }

        return folderContents;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceException.class)
    @ResponseBody
    ServerError resourceException(HttpServletRequest req, Exception ex) {
        return new ServerError(req.getRequestURL().toString(), ex); // FIXME
    }
}
