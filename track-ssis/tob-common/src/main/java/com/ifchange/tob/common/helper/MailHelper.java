package com.ifchange.tob.common.helper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ifchange.tob.common.support.DateFormat;
import com.sun.mail.util.MailSSLSocketFactory;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileTypeMap;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MailHelper {
    private static final Long DAY7 = 7 * DateHelper.DAY_TIME;
    private static final ExecutorService CONNECTOR = Executors.newSingleThreadExecutor();
    private static final String ICS_TYPE = "text/calendar;method=REQUEST;charset=\"UTF-8\"";
    private static final String SPLITTER = "&", SEPARATOR = "=", SENDER = "sender", NAME = "name", CHARSET = "UTF-8";
    private static final String DEFAULT_SENDER = "default.sender", DEFAULT_NAME = "default.name", HTML_T = "text/html;charset=UTF-8";
    private static final Cache<String, Pair<Session, Transport>> SMTP_CACHE = CacheBuilder.newBuilder()
                                                                                          .expireAfterWrite(DAY7, TimeUnit.MILLISECONDS)
                                                                                          .expireAfterAccess(DAY7, TimeUnit.MILLISECONDS).build();
    private MailHelper() { }

    /**
     * uri={SCHEME}://{USER}:{PWD}>>{HOST:PORT}?sender={SENDER}&name={NAME}
     * SCHEME=[smtp | smtps]
     */
    public static void send(String uri, M m) {
        // 当连接SMTP服务出错时，重试3次
        int tries = 0;
        do {
            Pair<Session, Transport> pair = smtpPair(uri);
            Transport transport = pair.getValue();
            if (transport.isConnected()) {
                MimeMessage message = mineMessage(pair.getKey(), m);
                try {
                    transport.sendMessage(message, message.getAllRecipients()); break;
                } catch (Exception e) {
                    throw new MailException("Transport mail error ", null != e.getCause() ? e.getCause() : e);
                }
            } else synchronized (SMTP_CACHE) {
                pair = smtpPair(uri);
                if (!pair.getValue().isConnected()) {
                    SMTP_CACHE.invalidate(uri);
                }
                tries += 1;
                try {
                    TimeUnit.SECONDS.sleep(1L);
                } catch (InterruptedException e) {
                    // continue to try send
                }
            }
        } while (tries < 3);
        if (tries >= 3) {
            throw new MailException("Can not connect mail smtp server please check your network...");
        }
    }

    public static Multipart createMultipart(Map<String, byte[]> attachments) {
        try {
            Multipart multipart = new MimeMultipart();
            List<String> icsList = Lists.newArrayList();
            for(Map.Entry<String, byte[]> fMap : attachments.entrySet()) {
                String fName = fMap.getKey(); byte[] fV = fMap.getValue();
                // ics 处理
                if (fName.endsWith(".ics") || fName.endsWith(".ICS")) {
                    icsList.add(fName);
                } else {
                    addMultipart(multipart, fName, fV);
                }
            }
            if (icsList.size() > 0) {
                if (1 == icsList.size()) {
                    BodyPart body = new MimeBodyPart();
                    //如果没有"text/calendar;method=REQUEST;charset=\"UTF-8\"，outlook会以附件的形式存在，而不是直接打开就是一个会议请求
                    body.setDataHandler(new DataHandler(new ByteArrayDataSource(BytesHelper.string(attachments.get(icsList.get(0))), ICS_TYPE)));
                    multipart.addBodyPart(body);
                } else {
                    for (String fName: icsList) {
                        addMultipart(multipart, fName, attachments.get(fName));
                    }
                }
            }
            return multipart;
        } catch (Exception e) {
            throw new MailException("Create multipart error ", null != e.getCause() ? e.getCause() : e);
        }
    }

    public static Multipart createMultipart(ICS ics) {
        try {
            BodyPart body = new MimeBodyPart();
            //如果没有"text/calendar;method=REQUEST;charset=\"UTF-8\"，outlook会以附件的形式存在，而不是直接打开就是一个会议请求
            body.setDataHandler(new DataHandler(new ByteArrayDataSource(ics.string(), ICS_TYPE)));
            Multipart multi = new MimeMultipart();
            multi.addBodyPart(body);
            return multi;
        } catch (Exception e) {
            throw new MailException("Create ics mail error ", e);
        }
    }

    private static Pair<Session, Transport> smtpPair(String uri) {
        Pair<Session, Transport> pair = SMTP_CACHE.getIfPresent(uri);
        if(null == pair) synchronized (SMTP_CACHE) {
            pair = SMTP_CACHE.getIfPresent(uri);
            if (null == pair) {
                final Smtp smtp = new Smtp(uri);
                final Future<Pair<Session, Transport>> task = CONNECTOR.submit(() -> {
                    Properties props = new Properties();
                    props.put("mail.smtp.timeout", DateHelper.MINUTE_TIME/2);
                    props.put("mail.smtp.writetimeout", DateHelper.MINUTE_TIME/2);
                    props.put("mail.smtp.auth", true);
                    props.put("mail.transport.protocol", "smtp");

                    props.put("mail.smtp.host", smtp.host);
                    props.put("mail.smtp.port", smtp.port);
                    if (smtp.ssl) {
                        MailSSLSocketFactory msf = new MailSSLSocketFactory();
                        msf.setTrustAllHosts(true);
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.ssl.socketFactory", msf);
                    }
                    props.put(DEFAULT_NAME, StringHelper.defaultString(smtp.name));
                    props.put(DEFAULT_SENDER, StringHelper.defaultString(smtp.sender));
                    Session session = Session.getInstance(props);
                    Transport transport = session.getTransport();
                    transport.connect(smtp.username, smtp.password);
                    return new Pair<>(session, transport);
                });
                try {
                    pair = task.get(DateHelper.MINUTE_TIME/5, TimeUnit.MILLISECONDS);
                    SMTP_CACHE.put(uri, pair);
                } catch (Exception e) {
                    if (e instanceof MailException) {
                        throw (MailException) e;
                    } else if (e instanceof TimeoutException) {
                        throw new MailException("Connect to SMTP host: " + smtp.host + " port: " + smtp.port + "  timeout error ");
                    } else {
                        throw new MailException("Create mail smtp session transport error ", null != e.getCause() ? e.getCause() : e);
                    }
                } finally {
                    if (null != task) {
                        task.cancel(true);
                    }
                }
            }
        }
        return pair;
    }

    private static MimeMessage mineMessage(Session session, M m) {
        try {
            MimeMessage mine = new MimeMessage(session);
            String from = session.getProperty(DEFAULT_SENDER);
            String name = session.getProperty(DEFAULT_NAME);
            mine.setFrom(new InternetAddress(from, name, CHARSET));
            // 发送
            if (m.toSet.size() > 0) {
                for(String to: m.toSet) {
                    mine.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to, shortName(to), CHARSET));
                }
            }
            // 抄送
            if (m.ccSet.size() > 0) {
                for(String cc: m.ccSet) {
                    mine.addRecipient(MimeMessage.RecipientType.CC, new InternetAddress(cc, shortName(cc), CHARSET));
                }
            }
            // 密送
            if (m.bccSet.size() > 0) {
                for(String bcc: m.bccSet) {
                    mine.addRecipient(MimeMessage.RecipientType.BCC, new InternetAddress(bcc, shortName(bcc), CHARSET));
                }
            }
            //邮件主题
            if (!StringHelper.isBlank(m.subject)) {
                mine.setSubject(m.subject, CHARSET);
            }
            //邮件正文
            if (null != m.multipart) {
                MimeBodyPart text = new MimeBodyPart();
                text.setContent(StringHelper.defaultString(m.content), HTML_T);
                m.multipart.addBodyPart(text);
                mine.setContent(m.multipart);
            } else {
                if (!StringHelper.isBlank(m.content)) {
                    mine.setContent(m.content, HTML_T);
                }
            }
            //发送时间
            mine.setSentDate(DateHelper.now());
            return mine;
        } catch (Exception e) {
            throw new MailException("Create mail MimeMessage error ", null != e.getCause() ? e.getCause() : e);
        }
    }

    private static void addMultipart(Multipart multipart, String fName, byte[] fV) throws MessagingException {
        BodyPart attachment = new MimeBodyPart();
        String ct = FileTypeMap.getDefaultFileTypeMap().getContentType(fName);
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(fV, ct)));
        attachment.setFileName(fName);
        multipart.addBodyPart(attachment);
    }

    private static void addIfValid(Set<String> mailSet, String email) {
        if(MatcherHelper.isEmail(email)) {
            mailSet.add(email);
        } else {
            throw new MailException("TO email=" + email + " not conform to mailbox format...");
        }
    }
    private static String shortName(String email) {
        return email.substring(0, email.indexOf("@"));
    }

    public static final class M {
        // 发送
        private Set<String> toSet = Sets.newHashSet();
        // 抄送
        private Set<String> ccSet = Sets.newHashSet();
        // 密送
        private Set<String> bccSet = Sets.newHashSet();
        // 邮件主题
        private String subject;
        // 邮件正文
        private String content;
        // 邮件附件
        private Multipart multipart;

        private M() { }
        public static M newborn() {
            return new M();
        }
        public M addToEmail(String email) {
            addIfValid(this.toSet, email);
            return this;
        }

        public M addCcEmail(String email) {
            addIfValid(this.ccSet, email);
            return this;
        }
        public M addBccEmail(String email) {
            addIfValid(this.bccSet, email);
            return this;
        }
        public M ofSubject(String subject) {
            this.subject = StringHelper.defaultString(subject);
            return this;
        }
        public M ofContent(String content) {
            this.content = StringHelper.defaultString(content);
            return this;
        }
        public M ofMultipart(Multipart multipart) {
            this.multipart = multipart;
            return this;
        }
    }

    public static class ICS implements Serializable {
        private static final long serialVersionUID = 6830708713664391570L;
        // 结束时间
        private final Date start;
        // 开始时间
        private final Date end;
        // 唯一ID
        private String uuid;
        // 是否取消
        private Boolean cancel;
        // 主题
        private String summary;
        // 地址
        private String location;
        // 重要性
        private Integer priority;
        // 会议描述
        private String description;
        // 提醒消息
        private String alarmMessage;
        // 提前几分钟提醒
        private Integer alarmMinutes;
        // 参会者列表
        private Set<String> participants = Sets.newHashSet();

        private ICS(Date start, Date end) {
            this.start = start;
            this.end = end;
        }
        public static ICS newborn(Date start, Date end) {
            return new ICS(start, end);
        }
        public ICS addParticipant(String email, String name) {
            addIfValid(this.participants, email);
            return this;
        }
        public ICS ofUuid(String uuid) {
            this.uuid = uuid;
            return this;
        }
        public ICS ofSummary(String summary) {
            this.summary = summary;
            return this;
        }
        public ICS ofDescription(String description) {
            this.description = description;
            return this;
        }
        public ICS ofLocation(String location) {
            this.location = location;
            return this;
        }
        public ICS ofPriority(int priority) {
            this.priority = priority;
            return this;
        }
        public ICS ofCancel(boolean cancel) {
            this.cancel = cancel;
            return this;
        }
        public ICS ofAlarmMessage(String message) {
            this.alarmMessage = message;
            return this;
        }
        public ICS ofAlarmMinutes(int minutes) {
            this.alarmMinutes = minutes;
            return this;
        }

        private String method() {
            return (null != cancel && cancel) ? "CANCEL" : "REQUEST";
        }
        private static final DateFormat format = DateFormat.StrikeDateTime;
        private String dtStart() {
            if(null == start) {
                return formatDate(DateHelper.now());
            }
            return formatDate(start);
        }
        private String dtEnd() {
            if(null == end) {
                return formatDate(DateHelper.now());
            }
            return formatDate(end);
        }
        private String summary() {
            return StringHelper.defaultIfBlank(summary, "summary");
        }
        private String uuid() {
            return StringHelper.defaultIfBlank(uuid, SnowIdHelper.unique());
        }
        private String location() {
            return StringHelper.defaultIfBlank(location, "location");
        }
        private int priority() {
            return null == priority ? 0 : priority;
        }
        public String description() {
            return StringHelper.defaultIfBlank(description, "description");
        }
        private String alarmMessage() {
            return StringHelper.defaultIfBlank(alarmMessage, StringHelper.defaultIfBlank(summary, "Alarm"));
        }
        private int alarmMinutes() {
            return null == alarmMinutes ? 30 : alarmMinutes;
        }
        private String formatDate(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat(format.name());
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+:00:00"));
            return sdf.format(date)
                      .replace(" ", "T")
                      .replace("-", StringHelper.EMPTY)
                      .replace(":", StringHelper.EMPTY)
                    + "Z";
        }

        public String string() {
            StringBuilder builder = new StringBuilder("BEGIN:VCALENDAR")
                    .append("\nPRODID:-//Ben Fortuna//iCal4j 1.0//EN")
                    .append("\nVERSION:2.0")
                    .append("\nCALSCALE:GREGORIAN")
                    .append("\nMETHOD:").append(method())
                    .append("\nBEGIN:VEVENT")
                    .append("\nUID:").append(uuid())
                    .append("\nSUMMARY:").append(summary())
                    .append("\nDESCRIPTION:").append(description())
                    .append("\nLOCATION:").append(location())
                    .append("\nPRIORITY:").append(priority())
                    .append("\nDTSTART:").append(dtStart())
                    .append("\nDTEND:").append(dtEnd());
            if(participants.size() > 0) {
                for(String mailTo: participants) {
                    builder.append("\nATTENDEE;ROLE=REQ-PARTICIPANT;CN=").append(shortName(mailTo)).append(":MAILTO:").append(mailTo);
                }
            }
            builder.append("\nBEGIN:VALARM")
                   .append("\nTRIGGER:-PT").append(alarmMinutes()).append("M")
                   .append("\nACTION:DISPLAY")
                   .append("\nDESCRIPTION:").append(alarmMessage())
                   .append("\nEND:VALARM")
                   .append("\nEND:VEVENT")
                   .append("\nEND:VCALENDAR");
            return builder.toString();
        }
    }

    // {SCHEME}://{USER}:{PWD}>>{HOST:PORT}?sender={SENDER}&name={NAME}
    private static final class Smtp implements Serializable {
        private static final long serialVersionUID = -4031197214533489714L;

        private static final Logger LOG = LoggerFactory.getLogger(Smtp.class);
        private static final String SPLIT = ":", TO_SERVER = ">>";
        private final boolean ssl;
        private final String host;
        private final String port;
        private final String username;
        private final String password;
        private final String sender;
        private final String name;
        // URI={SCHEME}://{NAME}:{PWD}>>{HOST:PORT}/{SENDER}
        private Smtp(String uri) {
            LOG.info("SMTP uri: {}", uri);
            if(StringHelper.isBlank(uri)) {
                throw new MailException("SMTP uri must not null/empty.....");
            }
            try {
                this.ssl = uri.contains("s://");
                int pathIdx = uri.lastIndexOf("?");
                Map<String, String> snMap = StringHelper.map(uri.substring(pathIdx + 1), SPLITTER, SEPARATOR);
                this.sender = snMap.get(SENDER); this.name = snMap.get(NAME);
                String[] location = uri.substring(uri.indexOf(TO_SERVER) + 2, pathIdx).split(SPLIT);
                this.host = location[0]; this.port = location[1];
                String[] uis = StringHelper.substringBetween(uri, "://", TO_SERVER).split(SPLIT);
                this.username = uis[0]; this.password = uis[1];
            } catch (Exception e) {
                throw new MailException("SMTP uri must format with {SCHEME}://{USER}:{PWD}>>{HOST:PORT}?sender={SENDER}&name={NAME} ");
            }
        }
    }

    public static final class MailException extends RuntimeException {
        private MailException(String message) {
            super(message);
        }
        private MailException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
