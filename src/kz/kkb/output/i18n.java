package kz.kkb.output;

import org.dom4j.*;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXContentHandler;
import org.dom4j.io.SAXReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.util.Hashtable;

/**
 * транслятор, работающий аналогично
 * http://cocoon.apache.org/2.1/userdocs/transformers/i18n-transformer.html
 *
 * Возможно, проще было бы натравить переводчик на текстовый поток
 * и искать определенные теги. Но тогда мы не сможем работать с не-текстовыми
 * потоками (PDF). Кроме того, в xml-тегах мы можем получить более гибкое управление,
 * например указывать из какого словаря брать сообщение
 * и строить более сложные конструкции
 *  
 * Created by IntelliJ IDEA.
 * User: asgibnev
 * Date: 10.03.2005
 * Time: 16:52:47
 * To change this template use File | Settings | File Templates.
 */
public class i18n {
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static DocumentFactory docFactory=DocumentFactory.getInstance();
    private static Hashtable dicts=null;//список словарей
    private static String sDictDir;
    private static PrintStream log=System.out;
    //private static Hashtable dictsTime=null;//список таймстампов словарей
    /**
     * инициализируем словари,
     * выполняется один раз на приложение, при старте первой страницы
     * Проверяет наличие словарей в кеше и если они есть, то не делает повторной загрузки.
     * При необходимости перезагрузки словарей вызовите clear(), а затем init().
     *
     * @param dictDir - полный путь к папке словарей, обычно WEB-INF/data/dict 
     * @throws Exception
     */
    public static void init(String dictDir) throws Exception {
        if (dicts!=null) return;
        dicts=new Hashtable();
        sDictDir=dictDir;
        File fDictDir=new  File(dictDir);
        File fDicts[] = fDictDir.listFiles();
        for (int i=0;i<fDicts.length;i++) {
            if (fDicts[i].getName().endsWith(".xml")) addDict(fDicts[i]);
        }
        log.println("i18n init done");
    }
    /**
     * Реинициализация, т.е. повторная загрузка словарей.
     * Введена для удобства отладки страниц не нарушая сессий
     * @deprecated
     */
    public static void reinit() throws Exception {
        if (dicts!=null) dicts.clear();
        dicts=null;
        init(sDictDir);
    }

    /**
     * Очистка кеша словарей, выполняется перед повторной инициализацией
     */
    public static void clear() {
        if (dicts!=null) dicts.clear();
        dicts=null;
    }
    /**
     * внутренняя процедура добавления словаря в список
     * @param fDict
     */
    private static void addDict(File fDict) {
        String sDict=fDict.getName();
        log.println("i18n: loading dictionary "+sDict);
        try {
        SAXReader reader=new SAXReader();
        Document dict=reader.read(fDict);
        dicts.put(sDict,dict);
        } catch (Exception e) {
            log.println("i18n load error:"+e.toString());
        }
    }

    //метод заменяет элементы, требующие перевода, на текст в соответствии с текущим языком сессии
    public static Document translateXml(Document xml, String language) {
        try {
            Transformer transformer=transformerFactory.newTransformer();
            i18ElementHandler i18e=new i18ElementHandler(language);
            SAXContentHandler s=new SAXContentHandler(docFactory,i18e);

            DocumentSource src= new DocumentSource(xml);
            DocumentResult result=new DocumentResult(s);

            transformer.transform(src,result);
            return result.getDocument();
        } catch (Exception e) {
            e.printStackTrace(log);
            return xml;
        }
    }
    /**
     * Возвращает перевод по указанному ключу,
     * или сам ключ если перевод не получился.
     * Ключ может содержать имя словаря: dict:key
     * @param key
     * @param lang
     * @return
     */
    public static String translateText(String text,String key, String lang) {
        //System.out.println("i18n translating text, key="+key);
        int i=key.indexOf(":");
        Document dict=null;
        if (i>0) {
            //если обнаружено ':' то пробуем загрузить словарь
            String sDictName=key.substring(1,i);
            key=key.substring(i+1);
            dict=(Document)dicts.get(sDictName+"-"+lang+".xml");
        }
        try {
            //если словарь не найден, загружаем общий
            if (dict==null) dict=(Document)dicts.get("common-"+lang+".xml");
            Element message=(Element)dict.selectSingleNode("/catalogue/message[@key='"+key+"']");
            text=message.getText();
        } catch (Exception e) {
            log.println("i18n key not found: "+lang+": "+
                    "<message key=\""+key+"\">"+key+"</message>");
        }
        return text;
    }

    public static void setLog(String name) {
        try {
            log=new PrintStream(new FileOutputStream(name));
            log.println("Log file set");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
      <!-- message catalogue file for locale ... -->
        <catalogue xml:lang="locale">
         <message key="key">text</message>
         <message key="other_key">Other text</message>
         ....
        </catalogue>
    */
}

/**
 * обработчик элементов, начинающихся на i18n: и аттрибута i18n:attr
 * Элемент i18n:text
 *      удаляем сам элемент, а текст внутри него переводим и подключаем родителю
 * Аттрибут i18n:
 *      при необходимости транслировать текст в аттрибутах
 *      используем специальный аттрибут i18n:attr="attr1 attr2 ..."
 *      где перечисляем аттрибуты, требующие перевода.
 */
class i18ElementHandler implements ElementHandler {
    public String lang="ru";

    public i18ElementHandler(String language) {
        lang=language;
    }
    public void onEnd(ElementPath path) {
        Element el=path.getCurrent();
        if ("i18n".compareTo(el.getNamespacePrefix())==0 ) {
            //System.out.println("i18n el name="+el.getName());

            if ("text".compareTo(el.getName())==0) {
                //переводим текст и убираем тег <i18n:text key="key">
                String text=el.getText();
                String key=el.attributeValue("key");
                if (key==null) key=text;
                text=i18n.translateText(text,key,lang);
                //переносим текст в родителя
                el.getParent().addText(text);
                //убираем текущий элемент <i18n:text key="key">
                el.detach();
                return;
            }
        }
/*
        //обработка тега img ВРЕМЕННО !
        if ("img".compareTo(el.getName())==0) {
            String src=el.attributeValue("src");
            src=src.replaceFirst("$lang",lang);
            el.attribute("src").setValue(src);
        }
*/
        //обработка атррибута i18n:img - локализация картинок
        //если в img указано i18n:img=1 то заменяем в src $lang на указанный lang
        Attribute aImg=el.attribute("img");
        if (aImg!=null) {
            if ("i18n".compareTo(aImg.getNamespacePrefix())==0) {
                String src=el.attributeValue("src");
                src=src.replaceFirst("\\$lang",lang);
                el.attribute("src").setValue(src);
                aImg.detach();

                //System.out.println("i18n:img src="+src);
            }
        }

        //обработка атртрибутов i18n:attr - перевод указанных атррибутов
        Attribute aAttr=el.attribute("attr");
        if (aAttr!=null) {
            if ("i18n".compareTo(aAttr.getNamespacePrefix())==0) {
            //переводим аттрибуты
            String attr[]=aAttr.getValue().split(" ");
            for (int i=0;i<attr.length;i++) {
                try {
                    String text=el.attribute(attr[i]).getValue();
                    text=i18n.translateText(text,text,lang);
                    el.attribute(attr[i]).setValue(text);
                } catch (Exception e) {
                }
                }
            }
        }
    }

    public void onStart(ElementPath path) {
    }
}

