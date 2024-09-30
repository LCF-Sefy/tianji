package com.jd.platform.hotkey.common.model;

import com.jd.platform.hotkey.common.model.typeenum.MessageType;

import java.util.List;

/**
 * netty通信消息
 *
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class HotKeyMsg {
    private int magicNumber;

    private String appName;

    private MessageType messageType;

    private String body;

    private List<HotKeyModel> hotKeyModels;

    private List<KeyCountModel> keyCountModels;

    public HotKeyMsg(MessageType messageType) {
        this(messageType, null);
    }

    public HotKeyMsg(MessageType messageType, String appName) {
        this.appName = appName;
        this.messageType = messageType;
    }

    public HotKeyMsg() {
    }

    @Override
    public String toString() {
        return "HotKeyMsg{" +
                "magicNumber=" + magicNumber +
                ", appName='" + appName + '\'' +
                ", messageType=" + messageType +
                ", body='" + body + '\'' +
                ", hotKeyModels=" + hotKeyModels +
                ", keyCountModels=" + keyCountModels +
                '}';
    }

    public List<HotKeyModel> getHotKeyModels() {
        return hotKeyModels;
    }

    public void setHotKeyModels(List<HotKeyModel> hotKeyModels) {
        this.hotKeyModels = hotKeyModels;
    }

    public List<KeyCountModel> getKeyCountModels() {
        return keyCountModels;
    }

    public void setKeyCountModels(List<KeyCountModel> keyCountModels) {
        this.keyCountModels = keyCountModels;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
