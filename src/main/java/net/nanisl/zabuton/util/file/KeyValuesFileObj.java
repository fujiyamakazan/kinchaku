package net.nanisl.zabuton.util.file;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.nanisl.zabuton.util.KeyValueString;

/**
 * キーと値のペアを定義したテキストファイルを操作します。
 * @author fujiyama
 */
public abstract class KeyValuesFileObj implements Serializable {

    private static final long serialVersionUID = 1L;

    //    @SuppressWarnings("unused")
    private static final Logger log =  LoggerFactory.getLogger(KeyValuesFileObj.class);

    protected Utf8FileObj utf8File;

    protected List<KeyValueString> keyValueString = null;

    public KeyValuesFileObj(Utf8FileObj utf8File) {
        this.utf8File = utf8File;
        log.debug(utf8File.file.getAbsolutePath());
    }

    public KeyValuesFileObj(String pathname) {
        this(new Utf8FileObj(pathname));
    }

    protected abstract List<KeyValueString> read();

    public abstract void write();

    /**
     * キーを指定して値を返します。
     * @param key キー
     * @return 値。キーが無ければnull。
     */
    public String get(String key) {
        if (keyValueString == null) {
            this.keyValueString = read();
        }
        for (KeyValueString kv: this.keyValueString) {
            if (StringUtils.equals(kv.getKey(), key)) {
                return kv.getValue();
            }
        }
        return null;
    }

    /**
     * キーを指定して値を登録します。
     * @param key キー
     * @param value 値
     */
    public void set(String key, String value) {
        if (keyValueString == null) {
            this.keyValueString = read();
        }
        boolean exist = false;
        for (KeyValueString kv: this.keyValueString) {
            if (StringUtils.equals(kv.getKey(), key)) {
                kv.setValue(value);
                exist = true;
            }
        }
        if (exist == false) {
            this.keyValueString.add(new KeyValueString(key, value));
        }
    }
}
