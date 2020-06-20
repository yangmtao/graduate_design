//请求接口url,这里为域名+项目名+操作名（注册、登录等）
//请求类型（GET：获取数据；POST：添加数据；DELETE:删除数据；PUT:更新数据；)，默认GET
var type = "GET";
//请求接口名称
var target;
//Ajax携带数据
var data;
//用户数据
var phone;
var telNumber;
var password;
var confirm;
var checkCode;
var sms;
$(function() {

    //Registerhref
    $(".iLogin").click(function() {
        location.href = "login.html?iLogin=false"
    })

    //Register Btn
    $(".RegBtn").click(function() {
        $(".LoginPanel").fadeOut(1)
        $(".RegPanel").fadeIn(1).addClass('flipInX');
    })

    //Login Btn
    $(".LoginBtn").click(function() {
            $(".LoginPanel").fadeIn(1).addClass('flipInY');
            $(".RegPanel").fadeOut(1)
        })
        //Nav
    $(".navbar>li").hover(function() {
        if (!$(this).hasClass('active')) {
            $(this).children("a").css("background", "#0d452c").css('color', '#FFF').css("opacity", "0").animate({ opacity: '1' }, 'normal')
        }
        // alert("hover")
    })

    $(".navbar>li").mouseleave(function() {
        if (!$(this).hasClass('active')) {
            $(this).children("a").css("background", "transparent").css('color', '#000')
        }
        // alert("leave")
    })

    $(".navbar>li").click(function() {
            if ($(this).attr("data-title") == "true") {
                return;
            }
            $(this).addClass('active').siblings().removeClass('active')
            $(this).siblings().children("a").removeAttr("style");
            // alert("click")
        })
        //FocusButton
    $(".form-control").focus(function() {
        $(this).siblings('.input-lable').animate({ top: '-60px', fontSize: '14px' }, 250);
    })

    $(".form-control").blur(function() {
        if ($(this).val() == "")
            $(this).siblings('.input-lable').animate({ top: '-35px', fontSize: '20px' }, 250)
    })

    //FormSubmitButton
    $("div.floating-btn").click(function() {
        $(this).submit()
    })
})

//验证图形验证码并获取短信验证码
function getSms(){
    //手机号正则，正则有使用范围
var mobileRegExp=/^1[3456789]\d{9}$/;
   telNumber=$("#RtelNumber").val().trim();
   checkCode=$("#RcheckCode").val();
   console.log("telNumber and checkCode is:"+telNumber+","+checkCode);
   if(mobileRegExp.test(telNumber)&&(checkCode!=null&&checkCode!="")){
       type="GET";
       target="/getSms";
       data={
           "telNumber":telNumber,
           "checkCode":checkCode
       }
       doAjax(target,type,data,smsSuccess);
       
   }else{
       alert("您输入的手机号或者图形验证码有误");
   }
}
//获取短信验证码成功函数
function smsSuccess(res){
    console.log("res is:"+res);
    //var data=JSON.parse(res);
	alert(res.details);
	RemainTime();
}
//倒计时函数
var iTime = 59;
function RemainTime(){
	
	var Account;
	document.getElementById('zphone').disabled = true;
	var iSecond,sSecond="",sTime="";
	if (iTime >= 0){
		iSecond = parseInt(iTime%60);
		iMinute = parseInt(iTime/60)
		if (iSecond >= 0){
			if(iMinute>0){
				sSecond = iMinute + "分" + iSecond + "秒";
			}else{
				sSecond = iSecond + "秒";
			}
		}
		sTime=sSecond;
		if(iTime==0){
			clearTimeout(Account);
			sTime='获取手机验证码';
			iTime = 59;
			document.getElementById('zphone').disabled = false;
		}else{
			Account = setTimeout("RemainTime()",1000);
			iTime=iTime-1;
		}
	}else{
		sTime='没有倒计时';
	}
	document.getElementById('zphone').innerHTML = sTime;
}
//执行注册
function userRegister() {
    sms=$("#Rsms").val();
    password = $("#Rpassword").val();
    confirm=$("#Rconfirm").val();
    telNumber=$("#RtelNumber").val();
    var registerForm=$("#Register input");
    var mobileRegExp=/^(86)?((13\d{9})|(15[0,1,2,3,4,5,6,7,8,9]\d{8})|(18[0,5,6,7,8,9]\d{8}))$/;
    for(i=0;i<registerForm.length;i++){
        if(registerForm[i].value==""||registerForm[i].value==null){
            console.log(registerForm[i].title);
            alert(registerForm[i].title+"不能为空");
            registerForm[i].focus();
            return false;
        }
    }
    if(!mobileRegExp.test(telNumber)){
        alert("请输入正确的手机号"+telNumber);
        return false;
    }
    if(password!=confirm){
        alert("两次密码不一致");
        return false;
    }
    target = "/user/register";
    type="POST";
    data={
        "phone":telNumber,
        "password":password,
        "sms":sms
    }
    //发起jQuery的Ajax请求
    var resData=doAjax(target,type,data,registerS);
}
//注册成功函数
function registerS(res){
    var resData=res;
    if(resData!=null){
        alert(resData.details);
    }else{
        alert("注册失败");
    }
}
//执行登录
function userLogin(event) {
    phone = $("#LtelNumber").val();
    password = $("#Lpassword").val();
    if(phone==""||password==""){
        alert("请输入完整的手机号或者密码");
        return false;
    }
    $.post("/login",{phone:phone,password:password},function(token){
        if(token=="fail"){
            alert("用户名或者密码错误");
        }else{
            // 验证token是否为空或者异常
            if(token==null){
                alert("服务器错误，请稍后再试！");
                return;
            }
            var returnUrl=$("#ReturnUrl").val();
            if(returnUrl==null||returnUrl==""){
                window.location.href="https://ymtao.xyz";
            }
            else{
                if(returnUrl.indexOf("?")!=-1){
                    window.location.href=returnUrl+"&token="+token;
                }else{
                    window.location.href=returnUrl+"?token="+token;
                }
            }
        }
    });
}

//输出Ajax请求结果详情
function printAjaxResult(data){
    console.log(target+"执行结果:"+data.details);
}

//ajax封装函数，successed为请求成功后执行的函数名称
function doAjax(target,type,data,func){
    $.ajax({
        type:type,
        url:target,
        //跨域session不一致处理
        /*xhrFields: {
          withCredentials: true  
          } ,*/
        dataType:"json",
        async: true,
       // crossDomain: true,
        //调用接口携带的数据
        data:data,
        success:function(res){
            func(res);
        },
        error:function(e){
        	alert("请求失败，系统可能维护中！")
        }
    })
}

