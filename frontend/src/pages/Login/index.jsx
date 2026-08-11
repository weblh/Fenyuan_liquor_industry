import { useEffect, useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { fetchCaptcha, login, fetchUserInfo } from '@/store/modules/user'
import styles from './index.module.css'

export default function Login() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const title = useSelector((s) => s.app.title)
  const captcha = useSelector((s) => s.user.captcha)
  const token = useSelector((s) => s.user.token)
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  const loadCaptcha = () => {
    dispatch(fetchCaptcha())
  }

  useEffect(() => {
    loadCaptcha()
  }, [])

  useEffect(() => {
    if (token) {
      navigate(searchParams.get('redirect') || '/dashboard', { replace: true })
    }
  }, [token])

  const onFinish = async (values) => {
    setLoading(true)
    try {
      const captchaKey = captcha?.captchaKey || captcha?.uuid || captcha?.key
      await dispatch(
        login({
          username: values.username,
          password: values.password,
          captcha: values.captcha,
          captchaKey,
        })
      ).unwrap()
      await dispatch(fetchUserInfo()).unwrap()
      message.success('登录成功')
      navigate(searchParams.get('redirect') || '/dashboard', { replace: true })
    } catch {
      loadCaptcha()
      form.setFieldsValue({ captcha: undefined })
    } finally {
      setLoading(false)
    }
  }

  const captchaImg =
    captcha?.captchaImage ||
    captcha?.img ||
    captcha?.image ||
    (captcha?.base64
      ? captcha.base64.startsWith('data:')
        ? captcha.base64
        : `data:image/png;base64,${captcha.base64}`
      : null)

  return (
    <div className={styles.page}>
      <div className={styles.overlay} />
      <Card className={styles.card} bordered={false}>
        <div className={styles.brand}>
          <div className={styles.mark}>汾</div>
          <h1 className={styles.title}>{title}</h1>
          <p className={styles.sub}>管理后台登录</p>
        </div>
        <Form form={form} size="large" onFinish={onFinish} initialValues={{ username: 'admin' }}>
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input prefix={<UserOutlined />} placeholder="用户名" allowClear />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 24 }}>
            <div className={styles.captchaRow}>
              <Form.Item name="captcha" noStyle rules={[{ required: true, message: '请输入验证码' }]}>
                <Input prefix={<SafetyOutlined />} placeholder="验证码" />
              </Form.Item>
              <div className={styles.captchaImg} onClick={loadCaptcha} title="点击刷新">
                {captchaImg ? (
                  <img src={captchaImg} alt="验证码" />
                ) : (
                  <span className={styles.captchaTip}>点击获取</span>
                )}
              </div>
            </div>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} className={styles.btn}>
              登 录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
