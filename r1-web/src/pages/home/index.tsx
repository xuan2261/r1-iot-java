import React, {useState, useEffect} from "react";
import {Form, Button, Row, Col, Card, Tabs, message} from "antd";
import {SoundOutlined, DeleteOutlined} from "@ant-design/icons";

import './home.css';
import {Device, R1AdminData, R1Resources} from "../../model/R1AdminData"; // 引入CSS文件
import {AxiosError} from 'axios';
import DeviceForm from "../../components/device";
import PasswordModal from "../../components/pop_pass";
import axiosInstance from "../../components/api";

const {TabPane} = Tabs;

const Box: React.FC = () => {
    const [r1AdminData, setR1AdminData] = useState<R1AdminData | null>(null);
    const [r1Resources, setR1Resources] = useState<R1Resources>();
    const [showPasswordModal, setShowPasswordModal] = useState(false); // Điều khiển hiển thị cửa sổ popup
    const [currentDeviceId, setCurrentDeviceId] = useState("");
    const [initValues, setInitValues] = useState<Device>();

    const [devices, setDevices] = useState<Device[]>([]);
    const [activeDeviceId, setActiveDeviceId] = useState<string>("");

    const [form] = Form.useForm();

    const apiURL = process.env.REACT_APP_API_URL;

    useEffect(() => {
        const activeDevice = devices.find((device) => device.id === activeDeviceId);
        console.log("activeDeviceId", activeDeviceId)

        if (activeDevice) {
            setInitValues(activeDevice);
            form.setFieldsValue(activeDevice);
        }
    }, [devices, activeDeviceId]); // Phụ thuộc vào devices và activeDeviceId

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await axiosInstance.get<R1AdminData>(`${apiURL}admin/resources`);
                const respData = response.data;
                setR1AdminData(respData);
                setDevices(respData.devices);
                setCurrentDeviceId(respData.currentDeviceId);
                setR1Resources(respData.r1Resources)



                if (!respData.devices.length) {
                    handleAddDevice([], respData.currentDeviceId);
                    return;
                }

                setActiveDeviceId(respData.devices[0].id);
                const currentDevice = respData.devices.find(item => item.id == respData.currentDeviceId)
                if (currentDevice) {
                    setActiveDeviceId(respData.currentDeviceId);
                } else {
                    handleAddDevice(respData.devices, respData.currentDeviceId);
                }

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


    const handleTabChange = (deviceId: string) => {
        console.log("change tab")
        setActiveDeviceId(deviceId);
        const activeDevice = devices.find((device) => device.id === deviceId);
        if (activeDevice) {
            setInitValues(activeDevice)
            form.setFieldsValue(activeDevice);
        }
    };


    const handleAddDevice = (myDevices: Device[], newId: string) => {
        console.log("new add")
        const newDevice: Device = {
            id: newId?newId:"",
            name: `Loa ${myDevices.length + 1}`,
            aiConfig: {
                choice: "Claude",
                key: "",
                systemPrompt:"Bạn là một loa thông minh hỗ trợ tiếng Việt",
                chatHistoryNum: 8
            },
            hassConfig: {
                endpoint: "",
                token: "",
            },
            newsConfig: {
                choice: "vietnamNews"
            },
            musicConfig: {
                choice: "NetEaseMusic"
            },
            audioConfig: {
                choice: "Youtube"
            },
            weatherConfig: {
                choice: "QWeatherService",
                endpoint: "",
                token: "",
                locationId: ""
            }
        };
        setDevices([...myDevices, newDevice]);
        if(!!newId){
            setActiveDeviceId(newDevice.id);
            setInitValues(newDevice);
            form.setFieldsValue(newDevice);
        }

    };

    const handleSaveDevice = async (values: Device) => {
        const updatedDevices = devices.map((device) =>
            device.id === activeDeviceId ? {...device, ...values} : device
        );
        setDevices(updatedDevices);

        try {
            await axiosInstance.post(`${apiURL}admin/device`, values)
            message.success("Lưu cấu hình thiết bị thành công!", 2);

        } catch (err) {
            const error = err as AxiosError; // Khẳng định kiểu là AxiosError
            if (error.response && error.response.status === 403) {
                setShowPasswordModal(true);
            } else {
                console.error('Error:', error.message);
            }
        }
    };

    const handleDeleteDevice = (deviceId: string) => {
        const confirmDelete = window.confirm("Bạn có chắc chắn muốn xóa thiết bị này không?");
        if (confirmDelete) {
            const updatedDevices = devices.filter((device) => device.id !== deviceId);
            setDevices(updatedDevices);
            if (deviceId === activeDeviceId && updatedDevices.length > 0) {
                const newActiveDeviceId = updatedDevices[0].id;
                setActiveDeviceId(newActiveDeviceId);
                setInitValues(updatedDevices[0])
                form.setFieldsValue(updatedDevices[0]);
            } else if (updatedDevices.length === 0) {
                setActiveDeviceId("");
                form.resetFields();
            }

            message.success("Đã xóa thiết bị", 2);
        }
    };



    return (
        <>
            {showPasswordModal && (
                <PasswordModal
                    onClose={() => setShowPasswordModal(false)}
                />
            )}

            <div className="container">

                <div className="card-container">

                    {r1Resources &&

                        <Row gutter={[16, 16]}>
                            <Col span={24}>
                                <Tabs activeKey={activeDeviceId} onChange={handleTabChange} type="card"
                                      tabBarStyle={{background: "#ececec", color: "#4d4d4d"}}>
                                    {devices.map((device) => (
                                        <TabPane
                                            tab={
                                                <>
                                                    <SoundOutlined style={{marginRight: 8, color: "red"}}/>
                                                    {device.name}
                                                    <Button
                                                        type="link"
                                                        icon={<DeleteOutlined/>}
                                                        onClick={() => handleDeleteDevice(device.id)}
                                                        style={{float: 'right', marginTop: -4}}
                                                    />
                                                </>
                                            }
                                            key={device.id}
                                        />
                                    ))}

                                </Tabs>
                            </Col>
                            <Col span={24}>
                                <Card title="Cấu hình thiết bị" style={{backgroundColor: "#ffffff", borderRadius: "8px"}}>
                                    <DeviceForm handleSaveDevice={handleSaveDevice} initValues={initValues}
                                                r1Resources={r1Resources} formInstance={form}/>
                                </Card>
                            </Col>
                        </Row>


                    }


                </div>
            </div>
        </>


    );
};

export default Box;
