package kz.kkb.output;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.dom4j.*;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.apache.log4j.Logger;

import java.io.Writer;
import java.io.OutputStream;
import java.io.File;
import java.util.Hashtable;

public class XmlTransformer {

  public static String pathFOConfig = null;
  private static Logger logger=Logger.getLogger(XmlTransformer.class);
  private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
  private static Hashtable transformers = new Hashtable(24);
  private static Hashtable transformersTime = new Hashtable(24);

    /**
     * Очистка кеша трансформеров,
     * используется для сброса кеша при внесении изменений в дизайн
     */
    public static void clearCache() {
        transformers.clear();
        transformersTime.clear();
    }
    
/**
 * Добавить xsl в кеш. Проверяет есть ли уже в кеше и не изменился ли xsl.
 * Если xsl-файл изменился, то обновляет кеш.
 * @param xslUrl
 * @return
 * @throws Exception
 */
  public static Transformer addXslUrl(String xslUrl) throws Exception {
      File f=new File(xslUrl);
      long time=f.lastModified();
      Long oldtime=(Long)transformersTime.get(xslUrl);
      if (oldtime!=null) {
          if (oldtime.longValue()<time) {
              transformers.remove(xslUrl);
              logger.debug("xsl reload:"+xslUrl);
          }
      }

    if (!transformers.containsKey(xslUrl)) {
        //StreamSource src=new StreamSource(f);
        transformers.put(xslUrl, transformerFactory.newTransformer(new StreamSource(xslUrl)));
        transformersTime.put(xslUrl, new Long(time));
    }
    return (Transformer)transformers.get(xslUrl);
  }

  /**
   * Трансформирует xml
   * @param xml
   * @param xslUrl
   * @param stream
   */
  public static void parseDOM(Document xml, String xslUrl, Writer stream) {
    try {
      Transformer t = addXslUrl(xslUrl);
      DocumentSource source = new DocumentSource(xml);
      StreamResult result = new StreamResult(stream);
      t.transform(source, result);
    } catch (Exception e) {
      logger.error(e);
      //e.printStackTrace();
    }
  }

  /**
   * Трансформирует xml
   * @param xml
   * @param xslUrl
   * @param stream
   */
  public static void parseDOM(Document xml, String xslUrl, OutputStream stream) {
    try {
      Transformer t = addXslUrl(xslUrl);
      DocumentSource source = new DocumentSource(xml);
      StreamResult result = new StreamResult(stream);
      t.transform(source, result);
    } catch (Exception e) {
      logger.error(e);
//      e.printStackTrace(System.out);
    }
  }

  /**
   * Трансформирует xml
   * @param xml
   * @param xslUrl
   * @return Document
   */
  public static Document parseDOM(Document xml, String xslUrl) throws Exception{
    DocumentResult result = null;
    Transformer t = addXslUrl(xslUrl);
    DocumentSource source = new DocumentSource(xml);
    result = new DocumentResult();
    t.transform(source, result);
    return result.getDocument();
  }

  /**
   * ������� ������ FO � �������� ����� (��������� ��������� UTF-8)
   * @param xml
   * @param out
   */
/*  public static void parseFO(Document xml, OutputStream out) {
    try {
      String as = xml.asXML();
      ByteArrayInputStream bais = new ByteArrayInputStream(as.getBytes("UTF-8"));
      InputSource is = new InputSource(bais);
      driver.setInputSource(is);
//    driver.setLogger( log );
      driver.setRenderer(Driver.RENDER_PDF);
      driver.setOutputStream(out);
      Options options = new Options(new File(pathFOConfig));
      driver.run();
    } catch (Exception e) {
      Constants.err("XmlTransformer.parseFO(d,o): "+e);
      e.printStackTrace(System.out);
    }
  }
*/
  /**
   * ������� ������ FO � �������� ����� ������������ � ����� ��������
   * @param xml
   * @param out
   */
/*  public static void parseFOresponse(Document xml, HttpServletResponse response) throws ServletException, IOException {
    ServletOutputStream sos = response.getOutputStream();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    parseFO(xml, baos);
    byte[] buffer = baos.toByteArray();
    response.setContentLength(buffer.length);
    sos.write(buffer);
    sos.flush();
  }
  */
}
