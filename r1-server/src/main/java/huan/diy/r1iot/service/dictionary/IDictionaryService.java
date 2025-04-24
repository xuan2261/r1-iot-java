package huan.diy.r1iot.service.dictionary;

import huan.diy.r1iot.service.IWebAlias;

public interface IDictionaryService extends IWebAlias {
    /**
     * Tra cứu từ điển
     * @param word Từ cần tra cứu
     * @return Kết quả tra cứu
     */
    String lookup(String word);
}
