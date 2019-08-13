package fr.openent.gar.export;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.List;

public class XMLValidationHandler implements ErrorHandler {
    private List<String> warnings;
    private List<String> errors;
    private List<String> fatalErrors;
    private boolean valid;

    public XMLValidationHandler () {
        valid = true;
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.fatalErrors = new ArrayList<>();
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        this.warnings.add(exception.toString());
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        this.errors.add(exception.toString());
        valid = false;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        this.fatalErrors.add(exception.toString());
        valid = false;
    }

    public boolean isValid() {
        return valid;
    }

    public String report() {
        StringBuilder report = new StringBuilder();
        if (!warnings.isEmpty()) {
            report.append("\n=== ").append(warnings.size()).append(" warnings found ===\n");
            for (String warning: warnings) {
                report.append(warning).append("\n");
            }
        }

        if (!errors.isEmpty()) {
            report.append("\n=== ").append(errors.size()).append(" errors found ===\n");
            for (String error: errors) {
                report.append(error).append("\n");
            }
        }

        if (!fatalErrors.isEmpty()) {
            report.append("\n=== ").append(fatalErrors.size()).append(" fatal errors found ===\n");
            for (String fatalError: fatalErrors) {
                report.append(fatalError).append("\n");
            }
        }

        return report.toString();
    }
}
