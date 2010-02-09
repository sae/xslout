package kz.kkb.output;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.HTMLWriter;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.Options;
import org.apache.fop.messaging.MessageHandler;
import org.apache.avalon.framework.logger.*;
import org.xml.sax.InputSource;
import javax.servlet.http.*;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.File;
import java.io.Serializable;

/**
 * <pre>
 * Объект, отвечающий за преборазование и вывод данных.
 * Получает xml-документ и xsl-stylesheet.
 * Пример использования:
 * ResponseProcessor.getInstance(session,response).setParameters(root,skin,lang).writeHtml(xml,xsl)
 *
 * - Добавляет в xml сообщения об ошибках (параметры сессии error_txt, info_txt)
 * - Добавляет в xml параметры root, skinroot и lang
 * - Преобразует с пом. xsl
 * - Накладывает перевод с помощью i18n (требует инициализации !)
 * - Выдает готовый документ в response и выставляет content-type
 * </pre>
 */
public class XslResponseProcessor implements Serializable {
    protected HttpSession session;// @deprecated - session не сериализуемый, поэтому не должен сохраняться
    public HttpServletResponse response;// @deprecated - response не сериализуемый, поэтому не должен сохраняться
    public String lang="ru";//язык сессии
    public String root="/";//корень приложения
    public String skin="/skins/default";//стиль сессии
    public String fop_userconfig="config.xml";//файл конфигурации
    /**
     * Конструктор спрятан
     */
    protected XslResponseProcessor() {
    }
    /**
     * Основной метод - использовать вместо конструктора.
     * Проверяет наличие объекта в сессии, если еще нет, то создает.
     * Устанавливает текущий response.
     * Возвращает объект ResponseProcessor уровня сесии.
     * Устанавливает user config file = /WEB-INF/fop-pdf/config.xml
     * @param sess
     * @return
     */
    protected static XslResponseProcessor getInstance(HttpSession sess) {
        XslResponseProcessor rp=(XslResponseProcessor)sess.getAttribute("ResponseProcessor");
        if (rp == null) {
            rp=new XslResponseProcessor();
            sess.setAttribute("ResponseProcessor",rp);
        }
        return rp;
    }

    /**
     * Метод который возвращает объект с правильным response.
     * используйте его по умолчанию, или переопределите при наследовании.
     * @deprecated - response не сериализуемый, поэтому не должен сохраняться
     * @param sess
     * @param resp
     * @return
     */
    public static XslResponseProcessor getInstance(HttpSession sess, HttpServletResponse resp) {
        XslResponseProcessor rp=XslResponseProcessor.getInstance(sess);
        //старые действия, приводят к не-сериализуемости
        rp.session=sess;
        rp.response=resp;//выставляем респонс каждый раз ?
        rp.fop_userconfig=sess.getServletContext().getRealPath("/WEB-INF/fop-pdf/config.xml");
        return rp;
    }

    /**
     * Процедура установки параметров. Если параметр null, значение не меняется.
     * По умолчанию root="/" skin="/skins/default" lang="ru"
     */
    public XslResponseProcessor setParameters(String root, String skin, String lang) {
        if (skin!=null) this.skin=skin;
        if (lang!=null) this.lang=lang;
        if (root!=null) this.root=root;
        return this;
    }

    /**
     * Процедура установки текущего response
     * @deprecated - response не сериализуемый, поэтому не должен сохраняться
     * @param resp
     * @return
     */
    public XslResponseProcessor setResponse(HttpServletResponse resp) {
        response=resp;//выставляем респонс каждый раз ?
        return this;
    }

    /**
     * Трансформация документа в XHTML в соответствии с xsl
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writeHtml(HttpServletResponse response, Document xml, String xsl)
    throws Exception {
        xml = getResultXML(xml, xsl);
     //трансформируем в html
     response.setContentType("text/html");
     response.setCharacterEncoding("UTF-8");
     response.getWriter().write(xml.asXML());
        //HTMLWriter преобразует документ, из-за чего вылазит проблема с электронной подписью
        //textarea получает форматированный документ с лишними пробелами
     //HTMLWriter writer = new HTMLWriter(aaa);//(response.getWriter());
     //writer.write(xml);
    }
    /**
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writePdf(HttpServletResponse response, Document xml, String xsl) throws Exception {

        xml = getResultXML(xml, xsl);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //Construct driver
        Driver driver = new Driver();

        //Setup logger
        Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_ERROR);

        driver.setLogger(logger);
        MessageHandler.setScreenLogger(logger);

        //Setup Renderer (output format)
        driver.setRenderer(Driver.RENDER_PDF);
        driver.setOutputStream(out);

        //Load user configurations
        Options options = new Options();
        options.loadUserconfiguration(new File(fop_userconfig));

        driver.setInputSource(new InputSource(new StringReader(xml.asXML())));
        driver.run();

        byte[] content = out.toByteArray();
        response.setContentType("application/pdf");
        response.setCharacterEncoding("Windows-1251");
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }
    /**
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writeXls(HttpServletResponse response, Document xml, String xsl)
    throws Exception {
        xml = getResultXML(xml, xsl);
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        HTMLWriter writer = new HTMLWriter(response.getOutputStream());
        writer.write(xml);  
        writer.flush();

        //response.getWriter().write(xml.asXML());
    }
    /**
     * Трансформация документа в XHTML в соответствии с xsl
     * @deprecated - response не сериализуемый, поэтому не должен сохраняться
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writeHtml(Document xml, String xsl)
    throws Exception {
        xml = getResultXMLOld(xml, xsl);
     //трансформируем в html
     response.setContentType("text/html");
     response.setCharacterEncoding("UTF-8");
     response.getWriter().write(xml.asXML());
        //HTMLWriter преобразует документ, из-за чего вылазит проблема с электронной подписью
        //textarea получает форматированный документ с лишними пробелами
     //HTMLWriter writer = new HTMLWriter(aaa);//(response.getWriter());
     //writer.write(xml);
    }
    /**
     * @deprecated - response не сериализуемый, поэтому не должен сохраняться
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writePdf(Document xml, String xsl) throws Exception {

        xml = getResultXMLOld(xml, xsl);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //PortalSession pSession=PortalSession.getInstance(session);
        //скин
        //String skin=pSession.getSkin();
        //skin="/skins/"+skin;

        //Construct driver
        Driver driver = new Driver();

        //Setup logger
        Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_ERROR);

        driver.setLogger(logger);
        MessageHandler.setScreenLogger(logger);

        //Setup Renderer (output format)
        driver.setRenderer(Driver.RENDER_PDF);
        driver.setOutputStream(out);

        //Load user configurations
        Options options = new Options();
        options.loadUserconfiguration(new File(fop_userconfig));

        driver.setInputSource(new InputSource(new StringReader(xml.asXML())));
        driver.run();

        byte[] content = out.toByteArray();
        response.setContentType("application/pdf");
        response.setCharacterEncoding("Windows-1251");
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
        response.getOutputStream().flush();
    }
    /**
     * @deprecated - response не сериализуемый, поэтому не должен сохраняться
     * @param xml
     * @param xsl
     * @throws Exception
     */
    public void writeXls(Document xml, String xsl)
    throws Exception {
        xml = getResultXMLOld(xml, xsl);
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        HTMLWriter writer = new HTMLWriter(response.getOutputStream());
        writer.write(xml);  
        writer.flush();

        //response.getWriter().write(xml.asXML());
    }
    /**
     * старый метод трансформации, использует не-сериализуемые элементы
     * @deprecated
     * @param xml
     * @param xsl
     * @return
     * @throws Exception
     */
    private Document getResultXMLOld(Document xml, String xsl) throws Exception{
        Element root=xml.getRootElement();
        //ошибка, если есть
        String errorTxt=(String)session.getAttribute("error_txt");
        if (errorTxt!=null) root.addElement("error").addElement("message").addText(errorTxt);
        session.removeAttribute("error_txt");
        //сообщение, если есть
        String infoTxt=(String)session.getAttribute("info_txt");
        if (infoTxt!=null) root.addElement("info").addElement("message").addText(infoTxt);
        session.removeAttribute("info_txt");

        root.addAttribute("root", this.root);
        //стиль должен знать где лежат его ресурсы, чтобы строить правильные ссылки
        root.addAttribute("skinroot", this.root+skin);
        root.addAttribute("lang", lang);

        //строим выходной xhtml
        xml=XmlTransformer.parseDOM(xml,session.getServletContext().getRealPath(skin+"/"+xsl));

        //накладываем перевод
        xml=i18n.translateXml(xml, lang);
        return xml;
    }

    private Document getResultXML(Document xml, String xsl) throws Exception{
        //строим выходной xhtml
        xml=XmlTransformer.parseDOM(xml,xsl);

        //накладываем перевод
        xml=i18n.translateXml(xml, lang);
        return xml;
    }

    /**
     * Делает редирект путем выставления заголовка 302 Moved temp.
     * Отличие от стандартного ResponseProcessor.sendRedirect(response,) заключается в том,
     * что url не преобразуется в абсолютный. Это несоответствие RFC,
     * но иначе мы имеем проблемы с работой через eDirector
     * @param url
     */
    public static void sendRedirect(HttpServletResponse response,String url) {
        response.setStatus(302);
        response.setHeader("Location",response.encodeRedirectURL(url));
    }

}
