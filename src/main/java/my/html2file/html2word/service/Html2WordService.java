package my.html2file.html2word.service;

import my.html2file.utils.BaseUtils;
import my.html2file.utils.DownloadUtils;
import my.html2file.utils.FilesUtils;
import my.html2file.utils.PathUtils;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * html 转 word 服务
 *
 * @author 欧阳洁
 * @since 2018-06-14 11:31
 */
@Service
public class Html2WordService {
    /**
     * 解析生成PDF
     * @param pageUrl
     * @return
     */
    public String excute(String pageUrl) throws Exception {
        String outputPath = new StringBuffer("/output/").append(BaseUtils.getDateStr("yyyyMMdd")).append("/word/").append(BaseUtils.uuid2()).append(".doc").toString();
        boolean success = convert(pageUrl,outputPath);
        if (success) {
            return outputPath;
        } else {
            if(FilesUtils.isExistNotCreate(outputPath)){
                return outputPath;
            }else {
                throw new Exception("转化异常！[" + outputPath + "]");
            }
        }
    }

    /**
     * html 转 word
     * @param pageUrl
     * @param outputPath
     * @return
     */
    private boolean convert(String pageUrl,String outputPath){
        try {
            //拼一个标准的HTML格式文档
            String content = getHtmlDealed(pageUrl);
            InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
            String absoultOutputPath = PathUtils.getClassRootPath(outputPath);
            FilesUtils.checkFolderAndCreate(absoultOutputPath);
            OutputStream os = new FileOutputStream(absoultOutputPath);
            POIFSFileSystem fs = new POIFSFileSystem();
            //对应于org.apache.poi.hdf.extractor.WordDocument
            fs.createDocument(is, "WordDocument");
            fs.writeFilesystem(os);
            os.close();
            is.close();
            return true;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理html字符串
     * @param pageUrl
     * @return
     */
    private String getHtmlDealed(String pageUrl){
        String html = DownloadUtils.getContentFromUrl(pageUrl);
        if(BaseUtils.isBlank(html)){
            return html;
        }
        String domain = getDomainWithHttp(pageUrl);
        html = loadCssFileStrToHtml(domain,html);
        int indexHead = html.indexOf("<head>") + 6;
        StringBuffer newHtmlBuf = new StringBuffer("");
        if(indexHead > 0) {
            newHtmlBuf.append(html.substring(0, indexHead));
        }else {
            return html;
        }
        newHtmlBuf.append("<meta charset=\"UTF-8\">");

        return newHtmlBuf.append(html.substring(indexHead)).toString();
    }

    /**
     * 获取 带 http 的 domain
     * @param pageUrl
     * @return
     */
    private String getDomainWithHttp(String pageUrl){
        int mIndex = pageUrl.indexOf(":");
        if(mIndex > 0) {
            int firstXIndex = pageUrl.indexOf("/", mIndex + 3);
            if(firstXIndex > 0) {
                return pageUrl.substring(0, firstXIndex);
            }
        }
        return pageUrl;
    }

    /**
     * html文档对img标签进行处理，改成非img标签:[img={src}]格式化html文本用，保留图片
     * @param domain
     * @param htmlText
     * @return
     */
    private String loadCssFileStrToHtml(String domain,String htmlText){
        String linkCssHref = "";
        Pattern p_link;
        Matcher m_link;
        String regEx_img = "<link.*href\\s*=\\s*[\"|']{1}?([^\"|^']*?)[\"|']{1}[^>]*?>";
        p_link = Pattern.compile(regEx_img, Pattern.CASE_INSENSITIVE);
        m_link = p_link.matcher(htmlText);
        StringBuffer html_sb = new StringBuffer();
        while (m_link.find()) {
            // 得到<img .../>数据
            linkCssHref = m_link.group(1);
            if(BaseUtils.isBlank(linkCssHref) || !linkCssHref.endsWith(".css")){
                m_link.appendReplacement(html_sb,m_link.group());
            }else {
                if(linkCssHref.startsWith("//")){
                    if(domain.startsWith("https")){
                        linkCssHref = "https:" + linkCssHref;
                    }else {
                        linkCssHref = "http:" + linkCssHref;
                    }
                }else if(linkCssHref.startsWith("/")){
                    linkCssHref = domain + linkCssHref;
                }
                String cssStr = DownloadUtils.getContentFromUrl(linkCssHref);
                //*** 匹配<img>中的src数据,并替换为本地服务http的地址
                String styleCssTag = "<style>" + cssStr + "<style>";
                //*** 匹配<img>中的src数据,并替换为本地服务http的地址
                m_link.appendReplacement(html_sb, styleCssTag);
            }
        }
        m_link.appendTail(html_sb);
        return html_sb.toString();
    }
}
