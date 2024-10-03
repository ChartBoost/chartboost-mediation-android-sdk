package com.chartboost.sdk.test;

import static com.chartboost.sdk.internal.Libraries.CommonsIO.DEFAULT_BUFFER_SIZE;
import static com.chartboost.sdk.internal.Libraries.CommonsIO.EOF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.annotation.NonNull;

import com.chartboost.sdk.internal.Libraries.CommonsIO;
import com.chartboost.sdk.internal.Model.ConfigurationBodyFields;
import com.chartboost.sdk.internal.Model.DeviceBodyFields;
import com.chartboost.sdk.internal.Model.IdentityBodyFields;
import com.chartboost.sdk.internal.Model.PrivacyBodyFields;
import com.chartboost.sdk.internal.Model.ReachabilityBodyFields;
import com.chartboost.sdk.internal.Model.RequestBodyFields;
import com.chartboost.sdk.internal.Model.SessionBodyFields;
import com.chartboost.sdk.internal.Model.TimeSourceBodyFields;
import com.chartboost.sdk.internal.Telephony.Carrier;
import com.chartboost.sdk.internal.adType.AdType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Deprecated
public class TestUtils {

    private static Random random = new Random();

    public static final List<Boolean> eitherBoolean = Arrays.asList(false, true);

    public static final List<Boolean> anyBooleanTransition = Arrays.asList(false, true, false);

    /**
     * strings for which TextUtils.isEmpty and Android.isEmpty will return true
     */
    public static final List<String> emptyStrings = Arrays.asList("", null);

    public static final AdType[] impressionAdTypes = new AdType[]{
            AdType.Interstitial.INSTANCE,
            AdType.Rewarded.INSTANCE
    };

    private static String[] allMediaTypeStrings = new String[]{
            "image",
            "video",
            "playable",
            "gif",
            "these can be anything really"
    };

    /*
        Compare the contents of a File with a reference resource.
     */
    public static void assertFileContentsMatchResource(File actualFile,
                                                       String resourcePath) {
        try {
            byte[] fileContents = CommonsIO.INSTANCE.readFileToByteArray(actualFile);
            byte[] resourceContents = readResourceToByteArray(resourcePath);
            String reason = "Actual file contents do not match expected contents in resource.\n" +
                    "    Actual File:       " + actualFile.getCanonicalPath() + "\n" +
                    "    Expected Resource: " + resourcePath;

            // The assertThat() check is slow, because it uses reflection to compare each
            // element.  So do a fast Arrays.equals check first, and fall back to the slow
            // comparison for failures in order to display the actual vs expected.
            if (!Arrays.equals(fileContents, resourceContents))
                assertThat(reason, fileContents, is(equalTo(resourceContents)));
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static void assertFileContentsMatchByteArray(File actualFile,
                                                        byte[] expectedContents) {
        try {
            byte[] fileContents = CommonsIO.INSTANCE.readFileToByteArray(actualFile);

            // The assertThat() check is slow, because it uses reflection to compare each
            // element.  So do a fast Arrays.equals check first, and fall back to the slow
            // comparison for failures in order to display the actual vs expected.
            if (!Arrays.equals(fileContents, expectedContents)) {
                String reason = "Actual file contents do not match expected contents.\n" +
                        "    Actual File:       " + actualFile.getCanonicalPath() + "\n" +
                        "    Expected Contents: " + Arrays.toString(expectedContents);
                assertThat(reason, fileContents, is(equalTo(expectedContents)));
            }
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static void assertFileContentsMatchString(File actualFile,
                                                     String expectedContents) {
        try {
            String actualContents = readFileToString(actualFile);
            String reason = "Actual file contents do not match expected contents.\n" +
                    "    Actual File:       " + actualFile.getCanonicalPath() + "\n" +
                    "    Expected Contents: " + expectedContents;
            assertThat(reason, actualContents, is(equalTo(expectedContents)));
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    /*
        Compare the contents of a Byte Array with a reference resource.
     */
    public static void assertByteArrayMatchesResource(byte[] actual,
                                                      String resourcePath) {
        byte[] resourceContents = readResourceToByteArray(resourcePath);
        String reason = "Actual contents of byte array do not match expected contents in resource.\n" +
                "    Expected Resource: " + resourcePath;

        // The assertThat() check is slow, because it uses reflection to compare each
        // element.  So do a fast Arrays.equals check first, and fall back to the slow
        // comparison for failures in order to display the actual vs expected.
        if (!Arrays.equals(actual, resourceContents))
            assertThat(reason, actual, is(equalTo(resourceContents)));
    }

    public static byte[] readResourceToByteArray(String resourcePath) {
        try (InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull("No such resource " + resourcePath, is);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            Log.e("test", resourcePath+" to byte array: "+is.available());
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            return buffer.toByteArray();
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static String readResourceToString(String resourcePath) {
        try (InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            return toString(is);
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static JSONObject readResourceToJSONObject(String resourcePath) {
        try {
            String contents = readResourceToString(resourcePath);
            return new JSONObject(contents);
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    public static void writeResourceToFile(String resourcePath, File outputFile) {
        byte[] contents = readResourceToByteArray(resourcePath);
        writeByteArrayToFile(contents, outputFile);
    }

    public static void writeByteArrayToFile(byte[] contents, File outputFile) {
        try (OutputStream os = new FileOutputStream(outputFile)) {
            write(contents, os);
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static void assertAssetsInCache(List<AssetDescriptor> descriptors, File baseDir) {
        for (AssetDescriptor descriptor : descriptors) {
            descriptor.assertAssetInCache(baseDir);
        }
    }

    public static void assertAssetsInCache(AssetDescriptor[] descriptors, File baseDir) {
        for (AssetDescriptor descriptor : descriptors) {
            descriptor.assertAssetInCache(baseDir);
        }
    }

    public static void writeAssetResourcesToCache(List<AssetDescriptor> descriptors, File baseDir) {
        for (AssetDescriptor descriptor : descriptors) {
            descriptor.writeResourceToCache(baseDir);
        }
    }

    public static void writeAssetResourcesToCache(AssetDescriptor[] descriptors, File baseDir) {
        for (AssetDescriptor descriptor : descriptors) {
            descriptor.writeResourceToCache(baseDir);
        }
    }

    public static void writeStringToFile(File file, String contents) {
        try {
            writeStringToFile(file, contents, Charset.defaultCharset(), false);
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static void createNewFile(File file) {
        try {
            assertTrue("Create " + file.getAbsolutePath(), file.createNewFile());
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public static void denoteExceptionLogExpected(String s) {
        System.err.append("\nThe following exception is expected to be logged in this test:\n    ");
        System.err.append(s);
        System.err.append("\n");
    }

    /*
        Set a field by name.  Used to set final fields in mock objects.
     */
    public static void setFieldWithReflection(Object object, Class cls, String name, Object newValue) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            f.set(object, newValue);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new Error(ex);
        }
    }

    /*
        Get a field by name.  Used to set final fields in mock objects.
     */
    public static Object getFieldWithReflection(Object object, Class cls, String name) {
        Field f = null;
        if (name != null && cls != null) try {
            f = cls.getDeclaredField(name); // note: SecurityException runtime exception would only happen if we'd use different class loaders for some weird reason
        } catch (NoSuchFieldException ex) {
            throw new Error(ex);
        }
        if (f != null) f.setAccessible(true);
        Object o = null;
        if (object != null && f != null) try {
            o = f.get(object); // note: will get a runtime exception if ! object instanceof cls
        } catch (IllegalAccessException | ExceptionInInitializerError ex) {
            throw new Error(ex);
        }
        return o;
    }

    /*
        Return elements in a JSONArray as a List<String>.

        Require that the elements are actually Strings (no toString() conversion)
     */
    public static List<String> toStringList(JSONArray jsonArray) {
        try {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); ++i) {
                result.add((String) jsonArray.get(i));
            }
            return result;
        } catch (JSONException ex) {
            throw new Error(ex);
        }
    }

    public static String join(Object[] objects, String sep) {
        return join(Arrays.asList(objects), sep);
    }

    public static String join(Collection<?> objects, String sep) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object obj : objects) {
            if (first)
                first = false;
            else
                sb.append(sep);
            sb.append(obj);
        }
        return sb.toString();
    }

    public static Object walkToAny(JSONObject root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (current != null && current instanceof JSONObject)
                current = ((JSONObject) current).opt(key);
            else
                break;
        }
        return current;
    }

    public static JSONArray walkToArray(JSONObject root, String... keys) {
        return (JSONArray) walkToAny(root, keys);
    }


    public static JSONArray newJSONArrayFromElements(Object... elements) {
        return new JSONArray(Arrays.asList(elements));
    }

    public static List<String> toStringList(Iterator<String> strings) {
        List<String> list = new ArrayList<>();
        while (strings.hasNext()) {
            list.add(strings.next());
        }
        return list;
    }

    // java.time.Duration is JDK1.8
    static String describeDuration(final long sourceDuration, final TimeUnit sourceUnit) {
        long nsRemaining = sourceUnit.toNanos(sourceDuration);

        TimeUnit[] units = new TimeUnit[]{
                TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS, TimeUnit.NANOSECONDS
        };
        String[] unitString = new String[]{
                "d", "hr", "min", "s", "ms", "ns"
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < units.length; i++) {
            TimeUnit partUnit = units[i];
            long partCount = partUnit.convert(nsRemaining, TimeUnit.NANOSECONDS);
            if (partCount > 0) {
                if (sb.length() > 0)
                    sb.append(" ");
                sb.append(partCount);
                sb.append(unitString[i]);
                nsRemaining -= partUnit.toNanos(partCount);
            }
        }
        if (sb.length() == 0)
            sb.append("0ns");

        return sb.toString();
    }

    @SafeVarargs // I have no idea what I'm doing
    public static <T> T randomOf(Random r, T... elements) {
        int i = r.nextInt(elements.length);
        return elements[i];
    }

    public static Map<String, Object> flatten(JSONObject obj) {
        Map<String, Object> flattened = new HashMap<>();

        flatten(flattened, obj, "");

        return flattened;
    }

    private static void flatten(Map<String, Object> flattened, JSONObject obj, String parentPath) {
        for (Iterator<String> itr = obj.keys(); itr.hasNext(); ) {
            String key = itr.next();
            String path = parentPath.isEmpty() ? key : parentPath + "." + key;
            final Object value = obj.opt(key);
            flattened.put(path, value);
            if (value instanceof JSONObject)
                flatten(flattened, (JSONObject) value, path);
        }
    }

    @NonNull
    public static Answer<Void> assertSynchronizedAnswer(final Object shouldNotBeSynchronized) {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String reason = String.format("Call to %s.%s should happen while %s is holds synchronize lock",
                        invocation.getMethod().getDeclaringClass().getSimpleName(),
                        invocation.getMethod().getName(),
                        shouldNotBeSynchronized.getClass().getSimpleName());

                assertTrue(reason, Thread.holdsLock(shouldNotBeSynchronized));
                return null;
            }
        };
    }

    @NonNull
    Answer<Void> assertNotSynchronizedAnswer(final Object shouldNotBeSynchronized) {
        return new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String reason = String.format("Call to %s.%s should happen while %s is outside of synchronized block",
                        invocation.getMethod().getDeclaringClass().getSimpleName(),
                        invocation.getMethod().getName(),
                        shouldNotBeSynchronized.getClass().getSimpleName());

                assertFalse(reason, Thread.holdsLock(shouldNotBeSynchronized));
                return null;
            }
        };
    }

    public static int randomAnimationId() {
        return random.nextInt(8);
    }

    public static String randomMediaType() {
        int index = random.nextInt(allMediaTypeStrings.length);
        return allMediaTypeStrings[index];
    }

    public static String randomString(String uniquePrefix) {
        StringBuilder sb = new StringBuilder();
        sb.append(uniquePrefix);
        sb.append(':');
        for (int i = 0; i < 12; i++) {
            char ch = (char) ('A' + random.nextInt(26));
            sb.append(ch);
        }
        return sb.toString();
    }

    public static int smallRandomInt() {
        return random.nextInt(800);
    }

    public static int randomBetween(int first, int last) {
        return first + random.nextInt(last - first + 1);
    }


    /**
     * Writes a String to a file creating the file if it does not exist.
     *
     * @param file    the file to write
     * @param data    the content to write to the file
     * @param charset the charset to use, {@code null} means platform default
     * @param append  if {@code true}, then the String will be added to the
     *                end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static void writeStringToFile(final File file, final String data, final Charset charset,
                                         final boolean append) throws IOException {
        try (OutputStream out = openOutputStream(file, append)) {
            write(data, out, charset);
        }
    }

    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * </p>
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     * </p>
     *
     * @param file   the file to open for output, must not be {@code null}
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @return a new {@link FileOutputStream} for the specified file
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be written to
     * @throws IOException if a parent directory needs creating but that fails
     * @since 2.1
     */
    public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            final File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    /**
     * Writes chars from a <code>String</code> to bytes on an
     * <code>OutputStream</code> using the specified character encoding.
     * <p>
     * This method uses {@link String#getBytes(String)}.
     *
     * @param data    the <code>String</code> to write, null ignored
     * @param output  the <code>OutputStream</code> to write to
     * @param charset the charset to use, null means platform default
     * @throws NullPointerException if output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    public static void write(final String data, final OutputStream output, final Charset charset) throws IOException {
        if (data != null) {
            output.write(data.getBytes(Charsets.toCharset(charset)));
        }
    }

    /**
     * Writes bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
     *
     * @param data   the byte array to write, do not modify during output,
     *               null ignored
     * @param output the <code>OutputStream</code> to write to
     * @throws NullPointerException if output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    public static void write(final byte[] data, final OutputStream output)
            throws IOException {
        if (data != null) {
            output.write(data);
        }
    }

    /**
     * Reads the contents of a file into a String using the default encoding for the VM.
     * The file is always closed.
     *
     * @param file the file to read, must not be {@code null}
     * @return the file contents, never {@code null}
     * @throws IOException in case of an I/O error
     * @since 1.3.1
     * @deprecated 2.5 use {@link #readFileToString(File, Charset)} instead (and specify the appropriate encoding)
     */
    @Deprecated
    public static String readFileToString(final File file) throws IOException {
        return readFileToString(file, Charset.defaultCharset());
    }

    /**
     * Reads the contents of a file into a String.
     * The file is always closed.
     *
     * @param file        the file to read, must not be {@code null}
     * @param charsetName the name of the requested charset, {@code null} means platform default
     * @return the file contents, never {@code null}
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static String readFileToString(final File file, final Charset charsetName) throws IOException {
        try (InputStream in = CommonsIO.INSTANCE.openInputStream(file)) {
            return toString(in, Charsets.toCharset(charsetName));
        }
    }

    /**
     * Gets the contents of a <code>byte[]</code> as a String
     * using the specified character encoding.
     * <p>
     * Character encoding names can be found at
     * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
     *
     * @param input       the byte array to read from
     * @param charsetName the name of the requested charset, null means platform default
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs (never occurs)
     */
    public static String toString(final byte[] input, final String charsetName) throws IOException {
        return new String(input, Charsets.toCharset(charsetName));
    }

    /**
     * Gets the contents of an <code>InputStream</code> as a String
     * using the specified character encoding.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * </p>
     *
     * @param input   the <code>InputStream</code> to read from
     * @param charset the charset to use, null means platform default
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    public static String toString(final InputStream input, final Charset charset) throws IOException {
        try (final StringBuilderWriter sw = new StringBuilderWriter()) {
            copy(input, sw, charset);
            return sw.toString();
        }
    }

    /**
     * Copies bytes from an <code>InputStream</code> to chars on a
     * <code>Writer</code> using the specified character encoding.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * This method uses {@link InputStreamReader}.
     *
     * @param input        the <code>InputStream</code> to read from
     * @param output       the <code>Writer</code> to write to
     * @param inputCharset the charset to use for the input stream, null means platform default
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    public static void copy(final InputStream input, final Writer output, final Charset inputCharset)
            throws IOException {
        final InputStreamReader in = new InputStreamReader(input, Charsets.toCharset(inputCharset));
        copy(in, output);
    }

    /**
     * Copies chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     * <p>
     *
     * @param input  the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.3
     */
    public static long copyLarge(final Reader input, final Writer output) throws IOException {
        return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
    }

    /**
     * Copies chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedReader</code>.
     * <p>
     *
     * @param input  the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @param buffer the buffer to be used for the copy
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    public static long copyLarge(final Reader input, final Writer output, final char[] buffer) throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copies chars from a <code>Reader</code> to a <code>Writer</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     * <p>
     * Large streams (over 2GB) will return a chars copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of chars cannot be returned as an int. For large streams
     * use the <code>copyLarge(Reader, Writer)</code> method.
     *
     * @param input  the <code>Reader</code> to read from
     * @param output the <code>Writer</code> to write to
     * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    public static int copy(final Reader input, final Writer output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    /**
     * Gets the contents of an <code>InputStream</code> as a String
     * using the default character encoding of the platform.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @deprecated 2.5 use {@link #toString(InputStream, Charset)} instead
     */
    @Deprecated
    public static String toString(final InputStream input) throws IOException {
        return toString(input, Charset.defaultCharset());
    }

    public static class StringBuilderWriter extends Writer implements Serializable {

        private static final long serialVersionUID = -146927496096066153L;
        private final StringBuilder builder;

        /**
         * Constructs a new {@link StringBuilder} instance with default capacity.
         */
        public StringBuilderWriter() {
            this.builder = new StringBuilder();
        }

        /**
         * Constructs a new {@link StringBuilder} instance with the specified capacity.
         *
         * @param capacity The initial capacity of the underlying {@link StringBuilder}
         */
        public StringBuilderWriter(final int capacity) {
            this.builder = new StringBuilder(capacity);
        }

        /**
         * Constructs a new instance with the specified {@link StringBuilder}.
         *
         * <p>If {@code builder} is null a new instance with default capacity will be created.</p>
         *
         * @param builder The String builder. May be null.
         */
        public StringBuilderWriter(final StringBuilder builder) {
            this.builder = builder != null ? builder : new StringBuilder();
        }

        /**
         * Appends a single character to this Writer.
         *
         * @param value The character to append
         * @return This writer instance
         */
        @Override
        public Writer append(final char value) {
            builder.append(value);
            return this;
        }

        /**
         * Appends a character sequence to this Writer.
         *
         * @param value The character to append
         * @return This writer instance
         */
        @Override
        public Writer append(final CharSequence value) {
            builder.append(value);
            return this;
        }

        /**
         * Appends a portion of a character sequence to the {@link StringBuilder}.
         *
         * @param value The character to append
         * @param start The index of the first character
         * @param end   The index of the last character + 1
         * @return This writer instance
         */
        @Override
        public Writer append(final CharSequence value, final int start, final int end) {
            builder.append(value, start, end);
            return this;
        }

        /**
         * Closing this writer has no effect.
         */
        @Override
        public void close() {
            // no-op
        }

        /**
         * Flushing this writer has no effect.
         */
        @Override
        public void flush() {
            // no-op
        }


        /**
         * Writes a String to the {@link StringBuilder}.
         *
         * @param value The value to write
         */
        @Override
        public void write(final String value) {
            if (value != null) {
                builder.append(value);
            }
        }

        /**
         * Writes a portion of a character array to the {@link StringBuilder}.
         *
         * @param value  The value to write
         * @param offset The index of the first character
         * @param length The number of characters to write
         */
        @Override
        public void write(final char[] value, final int offset, final int length) {
            if (value != null) {
                builder.append(value, offset, length);
            }
        }

        /**
         * Returns the underlying builder.
         *
         * @return The underlying builder
         */
        public StringBuilder getBuilder() {
            return builder;
        }

        /**
         * Returns {@link StringBuilder#toString()}.
         *
         * @return The contents of the String builder.
         */
        @Override
        public String toString() {
            return builder.toString();
        }
    }

    public static class Charsets {

        //
        // This class should only contain Charset instances for required encodings. This guarantees that it will load
        // correctly and without delay on all Java platforms.
        //

        private static final SortedMap<String, Charset> STANDARD_CHARSET_MAP;

        static {
            SortedMap<String, Charset> standardCharsetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            standardCharsetMap.put(StandardCharsets.ISO_8859_1.name(), StandardCharsets.ISO_8859_1);
            standardCharsetMap.put(StandardCharsets.US_ASCII.name(), StandardCharsets.US_ASCII);
            standardCharsetMap.put(StandardCharsets.UTF_16.name(), StandardCharsets.UTF_16);
            standardCharsetMap.put(StandardCharsets.UTF_16BE.name(), StandardCharsets.UTF_16BE);
            standardCharsetMap.put(StandardCharsets.UTF_16LE.name(), StandardCharsets.UTF_16LE);
            standardCharsetMap.put(StandardCharsets.UTF_8.name(), StandardCharsets.UTF_8);
            STANDARD_CHARSET_MAP = Collections.unmodifiableSortedMap(standardCharsetMap);
        }

        /**
         * Constructs a sorted map from canonical charset names to charset objects required of every implementation of the
         * Java platform.
         * <p>
         * From the Java documentation <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">
         * Standard charsets</a>:
         * </p>
         *
         * @return An immutable, case-insensitive map from canonical charset names to charset objects.
         * @see Charset#availableCharsets()
         * @since 2.5
         */
        public static SortedMap<String, Charset> requiredCharsets() {
            return STANDARD_CHARSET_MAP;
        }

        /**
         * Returns the given Charset or the default Charset if the given Charset is null.
         *
         * @param charset A charset or null.
         * @return the given Charset or the default Charset if the given Charset is null
         */
        public static Charset toCharset(final Charset charset) {
            return charset == null ? Charset.defaultCharset() : charset;
        }

        /**
         * Returns a Charset for the named charset. If the name is null, return the default Charset.
         *
         * @param charsetName The name of the requested charset, may be null.
         * @return a Charset for the named charset
         * @throws java.nio.charset.UnsupportedCharsetException If the named charset is unavailable
         */
        public static Charset toCharset(final String charsetName) {
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        }

        /**
         * CharEncodingISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

        /**
         * <p>
         * Seven-bit ASCII, also known as ISO646-US, also known as the Basic Latin block of the Unicode character set.
         * </p>
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset US_ASCII = StandardCharsets.US_ASCII;

        /**
         * <p>
         * Sixteen-bit Unicode Transformation Format, The byte order specified by a mandatory initial byte-order mark
         * (either order accepted on input, big-endian used on output)
         * </p>
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset UTF_16 = StandardCharsets.UTF_16;

        /**
         * <p>
         * Sixteen-bit Unicode Transformation Format, big-endian byte order.
         * </p>
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset UTF_16BE = StandardCharsets.UTF_16BE;

        /**
         * <p>
         * Sixteen-bit Unicode Transformation Format, little-endian byte order.
         * </p>
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset UTF_16LE = StandardCharsets.UTF_16LE;

        /**
         * <p>
         * Eight-bit Unicode Transformation Format.
         * </p>
         * <p>
         * Every implementation of the Java platform is required to support this character encoding.
         * </p>
         *
         * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html">Standard charsets</a>
         * @deprecated Use Java 7's {@link java.nio.charset.StandardCharsets}
         */
        @Deprecated
        public static final Charset UTF_8 = StandardCharsets.UTF_8;
    }

    public static RequestBodyFields createTestBodyFields() {
        return new RequestBodyFields(
                "",
                "",
                new IdentityBodyFields(),
                new ReachabilityBodyFields(),
                new Carrier("", "", "", "", "", 1),
                new SessionBodyFields(),
                new TimeSourceBodyFields(0, 0, 0),
                new PrivacyBodyFields(),
                new ConfigurationBodyFields("", true, "2"),
                new DeviceBodyFields(),
                null
        );
    }
}
