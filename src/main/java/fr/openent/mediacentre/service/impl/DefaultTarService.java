package fr.openent.mediacentre.service.impl;

import fr.openent.mediacentre.service.TarService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class DefaultTarService implements TarService {
    private Logger log = LoggerFactory.getLogger(DefaultTarService.class);

    private static TarArchiveOutputStream getTarArchiveOutputStream(String name) throws IOException {
        TarArchiveOutputStream taos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(name)));
        // TAR has an 8 gig file limit by default, this gets around that
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        // TAR originally didn't support long file names, so enable the support for it
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        taos.setAddPaxHeadersForNonAsciiNames(true);
        return taos;
    }

    private String extractArchiveName(String baseFileName) {
        String[] result = baseFileName.split("_");
        if (result.length > 2) {
            result = Arrays.copyOf(result, result.length - 2);
            StringBuilder strBuilder = new StringBuilder();
            int max = result.length;
            for (int i = 0; i < max; i++) {
                strBuilder.append(result[i]);
                if( i < (max-1) ) {
                    strBuilder.append("_");
                }
            }
            baseFileName = strBuilder.toString();
        }
        return baseFileName;
    }

    @Override
    public void compress(String dirDest, File DataFolder, Handler<Either<String, JsonObject>> handler) {
        JsonObject result = new JsonObject();
        TarArchiveOutputStream out = null;
        try {
            log.info("Init create tar.gz archive");
            // Add data to out and flush stream
            File[] children = DataFolder.listFiles();
            if (children != null) {
                File first = children[0];
                String archiveName = extractArchiveName(first.getName()) + ".tar.gz";
                out = getTarArchiveOutputStream(dirDest + archiveName);
                result.put("archive", archiveName);
                for (File child : children) {
                    log.info("Add " + first.getName() +" on archive " + archiveName);
                    out.putArchiveEntry(new TarArchiveEntry(child, child.getName()));
                    try (FileInputStream in = new FileInputStream(child)) {
                        IOUtils.copy(in, out);
                    }
                    catch (Exception e) {
                        log.info("Add file to archive failed");
                        throw e;
                    }
                    out.closeArchiveEntry();
                }
            }
            else {
                log.info("Nothing to compress");
            }
        } catch (Exception e) {
            log.error("Create Tar failed more details :");
            log.error(e);
            handler.handle(new Either.Left<>(e.toString()));
        }

        if (out != null) {
            try {
                log.info("Compress ok");
                out.close();
                result.put("status", true);
                try {
                    String pathMd5 = dirDest + result.getString("archive");
                    String md5 = DefaultTarService.getMD5Checksum(pathMd5);
                    InputStream stream = new ByteArrayInputStream(md5.getBytes(StandardCharsets.UTF_8));
                    handler.handle(new Either.Right<>(result));
                } catch (Exception e) {
                    log.info("Can't generate md5");
                    e.printStackTrace();
                    handler.handle(new Either.Left<>(e.toString()));
                }




            } catch (IOException e) {
                log.info("Can't close TAR.GZ output stream");
                e.printStackTrace();
                handler.handle(new Either.Left<>(e.toString()));
            }
        }
        else {
            result.put("status", true);
            handler.handle(new Either.Right<>(result));
        }
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }


    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}