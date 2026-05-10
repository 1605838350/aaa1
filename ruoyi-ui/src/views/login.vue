<template>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <h3 class="title">{{title}}</h3>
      
      <!-- 角色切换标签 -->
      <div class="role-tabs">
        <div 
          class="role-tab" 
          :class="{ active: loginRole === 'student' }"
          @click="switchRole('student')"
        >
          学生登录
        </div>
        <div 
          class="role-tab" 
          :class="{ active: loginRole === 'teacher' }"
          @click="switchRole('teacher')"
        >
          教师登录
        </div>
      </div>

      <!-- 学生登录表单 -->
      <template v-if="loginRole === 'student'">
        <el-form-item prop="grade">
          <el-select 
            v-model="loginForm.grade" 
            placeholder="请选择年级"
            class="student-select"
            @change="onGradeChange"
          >
            <el-option 
              v-for="grade in gradeOptions" 
              :key="grade.value" 
              :label="grade.label" 
              :value="grade.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item prop="classNum">
          <el-select 
            v-model="loginForm.classNum" 
            placeholder="请选择班级"
            class="student-select"
            :disabled="!loginForm.grade"
          >
            <el-option 
              v-for="cls in classOptions" 
              :key="cls.value" 
              :label="cls.label" 
              :value="cls.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item prop="studentNum">
          <el-input
            v-model="loginForm.studentNum"
            type="text"
            auto-complete="off"
            placeholder="请输入学号"
            maxlength="2"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
      </template>

      <!-- 教师登录表单 -->
      <template v-else>
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="教师账号"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
      </template>

      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>

      <!-- 显示生成的学生账号 -->
      <div v-if="loginRole === 'student' && generatedAccount" class="account-preview">
        登录账号：{{ generatedAccount }}
      </div>

      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import request from '@/utils/request'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      // 登录角色：student-学生, teacher-教师
      loginRole: 'student',
      // 年级选项（1-6年级）
      gradeOptions: [
        { value: 1, label: '一年级' },
        { value: 2, label: '二年级' },
        { value: 3, label: '三年级' },
        { value: 4, label: '四年级' },
        { value: 5, label: '五年级' },
        { value: 6, label: '六年级' }
      ],
      // 班级选项（1-6班）
      classOptions: [
        { value: 1, label: '1班' },
        { value: 2, label: '2班' },
        { value: 3, label: '3班' },
        { value: 4, label: '4班' },
        { value: 5, label: '5班' },
        { value: 6, label: '6班' }
      ],
      loginForm: {
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: "",
        // 学生登录字段
        grade: null,
        classNum: null,
        studentNum: ''
      },
      // 动态校验规则
      loginRules: {},
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined
    }
  },
  computed: {
    // 生成的学生账号
    generatedAccount() {
      if (this.loginRole !== 'student') return ''
      const { grade, classNum, studentNum } = this.loginForm
      if (!grade || !classNum || !studentNum) return ''
      
      // 计算入学年份：当前年份 - 年级（9月入学，同一年级跨越两个自然年）
      // 1年级 = 当前年，2年级 = 当前年-1，以此类推
      const currentYear = new Date().getFullYear()
      const admissionYear = currentYear - grade
      
      // 格式化班级和学号（补零）
      const classStr = classNum.toString().padStart(2, '0')
      const studentStr = studentNum.toString().padStart(2, '0')
      
      return `${admissionYear}${classStr}${studentStr}`
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    },
    // 监听学生表单变化，自动更新登录账号
    'loginForm.grade'() {
      this.updateLoginUsername()
    },
    'loginForm.classNum'() {
      this.updateLoginUsername()
    },
    'loginForm.studentNum'() {
      this.updateLoginUsername()
    }
  },
  created() {
    this.getCode()
    this.getCookie()
    this.updateLoginRules()
  },
  methods: {
    // 切换登录角色
    switchRole(role) {
      this.loginRole = role
      // 重置表单
      this.$refs.loginForm.resetFields()
      this.loginForm.grade = null
      this.loginForm.classNum = null
      this.loginForm.studentNum = ''
      this.loginForm.username = ''
      this.loginForm.password = ''
      this.updateLoginRules()
    },
    // 更新登录校验规则
    updateLoginRules() {
      if (this.loginRole === 'student') {
        this.loginRules = {
          grade: [{ required: true, trigger: 'change', message: '请选择年级' }],
          classNum: [{ required: true, trigger: 'change', message: '请选择班级' }],
          studentNum: [{ required: true, trigger: 'blur', message: '请输入学号' }],
          password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
          code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
        }
      } else {
        this.loginRules = {
          username: [{ required: true, trigger: 'blur', message: '请输入账号' }],
          password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
          code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
        }
      }
    },
    // 年级变化时清空班级
    onGradeChange() {
      this.loginForm.classNum = null
    },
    // 更新登录用户名
    updateLoginUsername() {
      if (this.loginRole === 'student') {
        this.loginForm.username = this.generatedAccount
      }
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      if (username) {
        this.loginForm.username = username
      }
      if (password) {
        this.loginForm.password = decrypt(password)
      }
      if (rememberMe) {
        this.loginForm.rememberMe = Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return

        if (this.loginRole === 'student') {
          this.handleStudentLogin()
        } else {
          this.handleTeacherLogin()
        }
      })
    },
    // 学生独立登录（不走若依认证体系）
    handleStudentLogin() {
      this.updateLoginUsername()
      if (!this.loginForm.username) {
        this.$message.warning('请完整填写年级、班级和学号')
        return
      }

      this.loading = true
      request({
        url: '/student/login',
        method: 'post',
        data: {
          account: this.loginForm.username,
          password: this.loginForm.password
        }
      }).then(res => {
        if (res.code === 200) {
          // 保存学生信息到本地，包含token用于后续接口验证
          const studentData = res.student || {}
          studentData.token = res.token || ''
          localStorage.setItem('studentInfo', JSON.stringify(studentData))
          this.$message.success('登录成功')
          this.$router.push('/student')
        } else {
          this.$message.error(res.msg || '登录失败')
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    // 教师登录（走若依标准认证）
    handleTeacherLogin() {
      this.loading = true
      if (this.loginForm.rememberMe) {
        Cookies.set("username", this.loginForm.username, { expires: 30 })
        Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
        Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove('rememberMe')
      }
      this.$store.dispatch("Login", this.loginForm).then(() => {
        this.$router.push({ path: this.redirect || "/" })
      }).catch(() => {
        this.loading = false
        if (this.captchaEnabled) {
          this.getCode()
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.title {
  margin: 0px auto 20px auto;
  text-align: center;
  color: #707070;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  z-index: 1;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}

// 角色切换标签
.role-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
  
  .role-tab {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    cursor: pointer;
    font-size: 14px;
    color: #606266;
    position: relative;
    transition: all 0.3s;
    
    &:hover {
      color: #409eff;
    }
    
    &.active {
      color: #409eff;
      font-weight: 500;
      
      &::after {
        content: '';
        position: absolute;
        bottom: -1px;
        left: 20%;
        width: 60%;
        height: 2px;
        background-color: #409eff;
      }
    }
  }
}

// 学生选择器样式
.student-select {
  width: 100%;
  
  ::v-deep .el-input__inner {
    height: 38px;
    line-height: 38px;
  }
}

// 账号预览
.account-preview {
  margin-bottom: 15px;
  padding: 10px 15px;
  background-color: #f0f9ff;
  border: 1px solid #b3d8ff;
  border-radius: 4px;
  color: #409eff;
  font-size: 14px;
  text-align: center;
  font-weight: 500;
}

.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 38px;
}
</style>
