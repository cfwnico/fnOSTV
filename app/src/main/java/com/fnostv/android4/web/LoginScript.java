package com.fnostv.android4.web;

import com.fnostv.android4.config.ServerProfile;

import org.json.JSONObject;

public final class LoginScript {
    private LoginScript() {
    }

    public static String build(ServerProfile profile) {
        String user = JSONObject.quote(profile.username);
        String pass = JSONObject.quote(profile.password);
        return "javascript:(function(){"
                + "if(window.__fnostvLoginTried){return;}window.__fnostvLoginTried=true;"
                + "var user=" + user + ",pass=" + pass + ";"
                + "function visible(e){return e&&e.offsetParent!==null;}"
                + "function setValue(e,v){if(!e){return;}e.focus();e.value=v;"
                + "var ev=document.createEvent('HTMLEvents');ev.initEvent('input',true,true);e.dispatchEvent(ev);"
                + "ev=document.createEvent('HTMLEvents');ev.initEvent('change',true,true);e.dispatchEvent(ev);}"
                + "var inputs=document.getElementsByTagName('input'),u=null,p=null;"
                + "for(var i=0;i<inputs.length;i++){var t=(inputs[i].type||'').toLowerCase();"
                + "var n=((inputs[i].name||'')+' '+(inputs[i].id||'')+' '+(inputs[i].placeholder||'')).toLowerCase();"
                + "if(!p&&t==='password'&&visible(inputs[i])){p=inputs[i];}"
                + "if(!u&&visible(inputs[i])&&t!=='password'&&t!=='hidden'&&(n.indexOf('user')>=0||n.indexOf('account')>=0||n.indexOf('phone')>=0||n.indexOf('mail')>=0||n.indexOf('用户名')>=0||n.indexOf('账号')>=0)){u=inputs[i];}}"
                + "if(!u&&p){for(var j=0;j<inputs.length;j++){if(inputs[j]!==p&&visible(inputs[j])&&(inputs[j].type||'text').toLowerCase()!=='hidden'){u=inputs[j];break;}}}"
                + "setValue(u,user);setValue(p,pass);"
                + "setTimeout(function(){var buttons=document.querySelectorAll('button,input[type=submit],.login,.btn');"
                + "for(var k=0;k<buttons.length;k++){var text=(buttons[k].innerText||buttons[k].value||buttons[k].className||'').toLowerCase();"
                + "if(visible(buttons[k])&&(text.indexOf('login')>=0||text.indexOf('登录')>=0||text.indexOf('进入')>=0)){buttons[k].click();return;}}},300);"
                + "})()";
    }
}
