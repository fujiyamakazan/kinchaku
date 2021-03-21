package net.nanisl.zabuton.util.file;

import java.util.List;

import org.apache.wicket.util.lang.Generics;

import net.nanisl.zabuton.util.KeyValueString;

/**
 * セパレーターで分割された「キー」と「値」で構成される
 * テキストファイルを操作します。
 * デフォルトのセパレーターは[=]です。
 * ファイルのイメージ
 * ----------
 * キー1:値1～
 * キー2:値2～
 *
 * @author fujiyama
 */
public class SeparateKeyFileObj extends KeyValuesFileObj {

    private static final long serialVersionUID = 1L;

    private static final String SEPARATOR = "=";

    public SeparateKeyFileObj(Utf8FileObj utf8File) {
        super(utf8File);
    }

    public SeparateKeyFileObj(String pathname) {
        super(pathname);
    }

    @Override
    public List<KeyValueString> read() {
        List<KeyValueString> results = Generics.newArrayList();

        List<String> lines = utf8File.readLines();
        for (String line : lines) {
            String[] split = line.split(SEPARATOR);
            results.add(new KeyValueString(split[0], split[1]));
        }
        return results;
    }

    @Override
    public void write() {
        List<String> lines = Generics.newArrayList();
        for (KeyValueString keyValue : super.keyValueString) {
            lines.add(keyValue.getKey() + SEPARATOR + keyValue.getValue());
        }
        utf8File.writeListString(lines);
    }
}
