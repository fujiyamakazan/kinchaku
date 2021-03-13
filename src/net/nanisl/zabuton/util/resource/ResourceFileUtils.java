package net.nanisl.zabuton.util.resource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * リソースに関わる処理の集約.
 * @author fujiyama
 */
public class ResourceFileUtils {
    /**
     * リソースのテキストを読み込む.
     * ※ Jarファイルとしてエクスポートした状態にも対応
     * @param fileName ファイルの相対パス
     * @return テキスト
     */
    public static List<String> readResource(String fileName) {
        return readResource(ResourceFileUtils.class, fileName);
    }

    /**
     * baseのクラスファイルの場所を起点にリソースのテキストを読み込む.
     * ※ Jarファイルとしてエクスポートした状態にも対応
     * @param base 起点
     * @param fileName ファイルの相対パス
     * @return テキスト
     */
    public static List<String> readResource(Class<?> base, String fileName) {
        List<String> lines;
        BufferedReader reader = null;
        InputStream inputStream = null;
        try {
            inputStream = base.getResourceAsStream(fileName);
            if (inputStream == null) {
                throw new RuntimeException("ResourceAsStream is Null of " + fileName);
            }
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            lines = IOUtils.readLines(reader);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(reader);
        }
        return lines;
    }

    /**
     * リソースから文字列を取得する.
     * @param base リソースを検索するときの基準
     * @param fileName リソース名
     * @return 取得した文字列
     */
    public static String readResourceAsString(Class<?> base, String fileName) {
        List<String> lines = readResource(base, fileName);
        String text = "";
        for (String line : lines) {
            if (text.length() > 0) {
                //line += IOUtils.LINE_SEPARATOR;
                text += IOUtils.LINE_SEPARATOR;
            }
            text += line;
        }
        return text;
    }

    public static String readResourceSameLyerAsString(Class<?> base, String suffix) {
        return readResourceAsString(base, base.getSimpleName() + suffix);
    }

    /**
     * リソースからバイト配列を取得する.
     * @param base リソースを検索するときの基準
     * @param fileName リソース名
     * @return 取得したバイト配列
     */
    public static byte[] readFile(Class<?> base, String fileName) {

        try (InputStream inputStream = base.getResourceAsStream(fileName);) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            while (true) {
                if (inputStream == null) {
                    throw new RuntimeException("ResourceAsStream is Null of " + fileName);
                }
                int len = inputStream.read(buffer);
                if (len < 0) {
                    break;
                }
                bout.write(buffer, 0, len);
            }
            return bout.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * プログラム内のクラスを取得する.
     * ※ 実行可能jarでも使用可能
     */
    public static List<Class<?>> findClassesCompatibleJar(String rootPackageName) {
        try {
            rootPackageName = rootPackageName.replaceAll(Pattern.quote("."), "/");
            if (StringUtils.endsWith(rootPackageName, "/") == false) {
                rootPackageName += "/";
            }
            List<Class<?>> result = new ArrayList<Class<?>>();
            ClassLoader classLoader = ResourceFileUtils.class.getClassLoader();
            URL url = classLoader.getResource(rootPackageName);
            if (url == null) {
                throw new IllegalArgumentException(rootPackageName);
            }

            if (url.getProtocol().equals("jar")) {
                /* Jarの場合 */
                String[] aryPath = url.getPath().split(":");
                String path = aryPath[aryPath.length - 1].split("!")[0];

                /*
                 * Jarの保存場所に日本語が含まれるとURLエンコードされているため、
                 * デコードする。
                 */
                path = urlDecode(path);

                File fileJar = new File(path);
                try (JarFile jar = new JarFile(fileJar)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name != null && name.startsWith(rootPackageName)) {
                            String fullName = name;
                            fullName = fullName.replaceAll("/", ".");
                            if (isClassFile(fullName)) {
                                String fullNameWithoutExtension = fullName.substring(0,
                                    fullName.length() - ".class".length());
                                try {
                                    result.add(Class.forName(fullNameWithoutExtension));
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }

            } else {
                /* Jarでない場合 */
                result.addAll(findClasses(rootPackageName, new File(url.getFile())));
            }
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String urlDecode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return str;
    }

    private static List<Class<?>> findClasses(String packageName, File dir)
        throws ClassNotFoundException {
        packageName = packageName.replaceAll("/", ".");
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        ClassLoader classLoader = ResourceFileUtils.class.getClassLoader();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (String path : dir.list()) {
            File entry = new File(dir, path);
            if (entry.isFile() && isClassFile(entry.getName())) {
                classes.add(classLoader.loadClass(packageName + "."
                    + fileNameToClassName(entry.getName())));
            } else if (entry.isDirectory()) {
                classes.addAll(findClasses(packageName + "." + entry.getName(), entry));
            }
        }
        return classes;
    }

    private static boolean isClassFile(String fileName) {
        return fileName.endsWith(".class");
    }

    private static String fileNameToClassName(String name) {
        return name.substring(0, name.length() - ".class".length());
    }

}
