package core;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class FileTraceSink implements TraceSink, AutoCloseable {
    private final BufferedWriter out;

    public FileTraceSink(String base) throws IOException {
        String fname = base.endsWith(".trace") ? base : base + ".trace";
        this.out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fname), StandardCharsets.UTF_8));
    }

    @Override 
    public synchronized void onRetire(String line) {
        try {
            out.write(line);
            out.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            out.flush();
            out.close();
        } catch (IOException ignored) {}
    }
}
