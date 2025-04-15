package com.intellij.plugins.bodhi.pmd.core;

import com.google.gson.stream.JsonWriter;
import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import net.sourceforge.pmd.reporting.ViolationSuppressor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import static net.sourceforge.pmd.reporting.RuleViolation.PACKAGE_NAME;

/**
 * For exporting anonymized PMD results to a server in JSON format.
 * Can be tested e.g. with nc -lvk 8080.
 */
public class PMDJsonExportingRenderer extends AbstractIncrementingRenderer {
    private static final String NAME = "json exporter";
    private static final int FORMAT_VERSION = 0;
    private static final int SOCKET_TIMEOUT = 200; // no response expected, so can be short
    private static final int CONNECT_TIMEOUT = 200;
    private static final String USER_NAME_HASH = DigestUtils.sha1Hex(System.getProperty("user.name"));
    private static final String HOST_NAME_HASH;
    private static final String SESSION_ID = UUID.randomUUID().toString();
    public static final int INITIAL_CAPACITY = 2048;

    private JsonWriter jsonWriter;
    private final String exportStatisticsUrl;

    static {
        String hash;
        try { // we use sha1 to balance enough uniqueness and limit data size
            hash = DigestUtils.sha1Hex(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            hash = e.getMessage();
        }
        HOST_NAME_HASH = hash;
    }


    public PMDJsonExportingRenderer(String url) {
        super(NAME, "JSON format exporter of anonymous pmd results.");
        exportStatisticsUrl = url;
        setWriter(new StringWriter(INITIAL_CAPACITY));
    }

    @Override
    public String defaultFileExtension() {
        return "json";
    }

    @Override
    public void start() throws IOException {
        jsonWriter = new JsonWriter(writer);
        jsonWriter.setHtmlSafe(true);
        jsonWriter.setIndent("  ");
        jsonWriter.beginObject();
        jsonWriter.name("formatVersion").value(FORMAT_VERSION);
        jsonWriter.name("pmdVersion").value(PMDVersion.VERSION);
        jsonWriter.name("userNameHash").value(USER_NAME_HASH);
        jsonWriter.name("hostNameHash").value(HOST_NAME_HASH);
        jsonWriter.name("sessionId").value(SESSION_ID);
        jsonWriter.name("timestamp").value(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()));
        jsonWriter.name("files").beginArray();
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
        String filename = null;

        while (violations.hasNext()) {
            RuleViolation rv = violations.next();
            String nextFilename = determineFileName(rv.getFileId());
            if (!nextFilename.equals(filename)) {
                // New File
                if (filename != null) {
                    // Not first file ?
                    jsonWriter.endArray(); // violations
                    jsonWriter.endObject(); // file object
                }
                filename = nextFilename;
                jsonWriter.beginObject();
                String hashRootedPath = pathWithHashRoot(filename, rv);
                jsonWriter.name("hashRootedPath").value(hashRootedPath);
                jsonWriter.name("violations").beginArray();
            }
            renderSingleViolation(rv);
        }

        jsonWriter.endArray(); // violations
        jsonWriter.endObject(); // file object
    }

    private int sourceRootPos(String fullFileName) {
        final String sep = File.separator;
        int srcPos = fullFileName.indexOf(sep + "src") + 4;
        int mainPos = fullFileName.indexOf(sep + "main") + 5;
        int javaPos = fullFileName.indexOf(sep + "java") + 5;
        return Math.max(srcPos, Math.max(mainPos, javaPos));
    }

    private int sourceRootPos(String fullFileName, String packageName) {
        String packageAsPath = packageName.replace(".", File.separator);
        return fullFileName.indexOf(packageAsPath);
    }

    private String pathWithHashRoot(String filename, RuleViolation rv) {
        int srcRootPos;
        if (rv == null) {
            srcRootPos = sourceRootPos(filename);
        }
        else {
            srcRootPos = sourceRootPos(filename, rv.getAdditionalInfo().get(PACKAGE_NAME));
        }
        if (srcRootPos < 0) {
            srcRootPos = 0; // could not determine sourceRoot, take hash=0 and full file path.
        }
        return filename.substring(0, srcRootPos).hashCode() + File.separator + filename.substring(srcRootPos);
    }

    private void renderSingleViolation(RuleViolation rv) throws IOException {
        renderSingleViolation(rv, null, null);
    }

    private void renderSingleViolation(RuleViolation rv, String suppressionType, String userMsg) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("beginline").value(rv.getBeginLine());
        jsonWriter.name("begincolumn").value(rv.getBeginColumn());
        jsonWriter.name("rule").value(rv.getRule().getName());
        jsonWriter.name("ruleset").value(rv.getRule().getRuleSetName());
        jsonWriter.name("priority").value(rv.getRule().getPriority().getPriority());
        if (StringUtils.isNotBlank(suppressionType)) {
            jsonWriter.name("suppressiontype").value(suppressionType);
        }
        if (StringUtils.isNotBlank(userMsg)) {
            jsonWriter.name("usermsg").value(userMsg);
        }
        jsonWriter.endObject();
    }

    @Override
    public void end() throws IOException {
        jsonWriter.endArray(); // files

        jsonWriter.name("suppressedViolations").beginArray();
        String filename = null;
        if (!this.suppressed.isEmpty()) {
            for (Report.SuppressedViolation s : this.suppressed) {
                RuleViolation rv = s.getRuleViolation();
                String nextFilename = determineFileName(rv.getFileId());
                if (!nextFilename.equals(filename)) {
                    // New File
                    if (filename != null) {
                        // Not first file ?
                        jsonWriter.endArray(); // violations
                        jsonWriter.endObject(); // file object
                    }
                    filename = nextFilename;
                    jsonWriter.beginObject();

                    String hashRootedPath = pathWithHashRoot(filename, rv);
                    jsonWriter.name("hashRootedPath").value(hashRootedPath);
                    jsonWriter.name("violations").beginArray();
                }
                renderSingleViolation(rv, s.getSuppressor() == ViolationSuppressor.NOPMD_COMMENT_SUPPRESSOR ? "nopmd" : "annotation", s.getUserMessage());
            }
            jsonWriter.endArray(); // violations
            jsonWriter.endObject(); // file object
        }
        jsonWriter.endArray();

        // MAYDO exclude if no processing errors?
        jsonWriter.name("processingErrors").beginArray();
        for (Report.ProcessingError error : this.errors) {
            jsonWriter.beginObject();
            //jsonWriter.name("filename").value(error.getFile());
            String hashRootedPath = pathWithHashRoot(error.getFileId().getOriginalPath(), null);
            jsonWriter.name("hashRootedPath").value(hashRootedPath);
            String msg = error.getMsg();
            int posFile = msg.indexOf(error.getFileId().getOriginalPath());
            String msgWithoutFile = msg.substring(0, posFile);
            jsonWriter.name("message").value(msgWithoutFile);
            jsonWriter.name("cause").value(error.getError().getCause().getMessage());
            jsonWriter.endObject();
        }
        jsonWriter.endArray();

        // MAYDO exclude if no conf errors?
        jsonWriter.name("configurationErrors").beginArray();
        for (Report.ConfigurationError error : this.configErrors) {
            jsonWriter.beginObject();
            jsonWriter.name("rule").value(error.rule().getName());
            jsonWriter.name("ruleset").value(error.rule().getRuleSetName());
            jsonWriter.name("message").value(error.issue());
            jsonWriter.endObject();
        }
        jsonWriter.endArray();

        jsonWriter.endObject();
        jsonWriter.flush();
    }

    /**
     * Export the Json formatted data
     * @return an error message in case of failure, empty String in case of success
     */
    public String exportJsonData() {
        String content = getWriter().toString();
        return tryJsonExport(content, exportStatisticsUrl); // we assume it works
    }

    /**
     * Try export Json formatted content to specified URL and return error description in case of failure.
     * @param content the Json formatted content to export
     * @param url the URL of the endpoint to export to
     * @return an error message in case of failure, empty String in case of success
     */
    public static String tryJsonExport(String content, String url) {
        String msg = "";
        HttpPost httpPost = new HttpPost(url);
        StringEntity contentEntity = new StringEntity(content,
                ContentType.create("application/json", "UTF-8"));
            httpPost.setEntity(contentEntity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(CONNECT_TIMEOUT).setConnectTimeout(CONNECT_TIMEOUT)
                    .setSocketTimeout(SOCKET_TIMEOUT).build();
            try (CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
                 CloseableHttpResponse ignored = client.execute(httpPost)) {
            } catch (SocketTimeoutException e) {
            // no-op, expected because no response back
        }
        catch (IOException e) {
            //e.printStackTrace(); - not needed
            if (e.getCause() != null) {
                msg = e.getCause().getMessage();
            }
            else {
                msg = e.getMessage();
            }
        }
        if (msg.equals("Connection refused (Connection refused)")) {
            msg = "Connection refused";
        }
        return msg;
    }
}

