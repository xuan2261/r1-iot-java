import React, {useEffect, useState} from "react";
import PasswordModal from "../../components/pop_pass";
import {Button, Col, Form, Input, message, Row} from "antd";
import axiosInstance from "../../components/api";
import {R1AdminData, R1GlobalConfig} from "../../model/R1AdminData";
import {AxiosError} from "axios";

const Server: React.FC = () => {
    const [showPasswordModal, setShowPasswordModal] = useState(false); // Điều khiển hiển thị cửa sổ popup
    const [r1AdminData, setR1AdminData] = useState<R1AdminData | null>(null);

    const apiURL = process.env.REACT_APP_API_URL;

    const [form] = Form.useForm();

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await axiosInstance.get<R1AdminData>(`${apiURL}admin/resources`);
                const respData = response.data;
                setR1AdminData(respData);

                console.log("r1AdminData", respData)

                form.setFieldsValue(respData.r1GlobalConfig);
            } catch (err) {
                const error = err as AxiosError; // Khẳng định kiểu là AxiosError
                if (error.response && error.response.status === 403) {
                    setShowPasswordModal(true);
                } else {
                    console.error('Error:', error.message);
                }
            }
        };

        fetchData();
    }, [])

    const onFinish = async (values: R1GlobalConfig) => {

        try {
            await axiosInstance.post(`${apiURL}admin/globalConfig`, values)
            message.success("Cấu hình máy chủ đã được cập nhật!", 2);

        } catch (err) {
            const error = err as AxiosError; // Khẳng định kiểu là AxiosError
            if (error.response && error.response.status === 403) {
                setShowPasswordModal(true);
            } else {
                console.error('Error:', error.message);
            }
        }
    }

    return (
        <>
            {showPasswordModal && (
                <PasswordModal
                    onClose={() => setShowPasswordModal(false)}
                />
            )}

            <div className="container">

                <div className="card-container">

                    {r1AdminData &&

                        <Row gutter={[16, 16]}>
                            <Col span={24}>
                                <Form
                                    style={{"padding":"10px"}}
                                    form={form}
                                    layout="vertical"
                                    onFinish={onFinish}
                                    initialValues={r1AdminData.r1GlobalConfig}
                                >
                                    <Form.Item
                                        label="IP máy chủ"
                                        name="hostIp"
                                        rules={[{message: 'Vui lòng nhập IP máy chủ'}]}
                                    >
                                        <Input placeholder="Ví dụ: 192.168.1.100:8080, đảm bảo loa R1 có thể truy cập địa chỉ này"/>
                                    </Form.Item>

                                    <Form.Item
                                        label="Điểm cuối yt-dlp"
                                        name="ytdlpEndpoint"
                                        rules={[{message: 'Vui lòng nhập điểm cuối yt-dlp'}]}
                                    >
                                        <Input placeholder="Ví dụ: http://example.com:5000, máy ARM thực thi yt-dlp chậm hơn"/>
                                    </Form.Item>

                                    <Form.Item
                                        label="ID dịch vụ Cloudflare"
                                        name="cfServiceId"
                                        rules={[{message: 'Vui lòng nhập ID dịch vụ Cloudflare'}]}
                                    >
                                        <Input placeholder="Ví dụ: service-123, cung cấp kết nối xuyên mạng nội bộ"/>
                                    </Form.Item>

                                    <Form.Item>
                                        <div style={{display: 'flex', justifyContent: 'flex-end', gap: '12px'}}>

                                            <Button type="primary" htmlType="submit">
                                                Lưu cấu hình
                                            </Button>
                                        </div>
                                    </Form.Item>
                                </Form>
                            </Col>
                        </Row>


                    }


                </div>
            </div>
        </>


    );

}

export default Server;