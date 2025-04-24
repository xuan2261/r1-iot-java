import React, {useEffect, useState} from "react";
import {Button, Col, Collapse, Form, FormInstance, Input, InputNumber, message, Row, Select, Space, Tabs} from "antd";
import {SaveOutlined} from "@ant-design/icons";
import {Device, R1Resources} from "../model/R1AdminData";

const {Option} = Select;
const {Panel} = Collapse;

interface DeviceFormProps {
    devices?: Device[];  // devices will be an array of Device objects
    handleSaveDevice: (device: Device) => void;
    r1Resources: R1Resources;
    formInstance: FormInstance<Device>;
    initValues?: Device;
}

const DeviceForm: React.FC<DeviceFormProps> = ({handleSaveDevice, initValues, r1Resources, formInstance}) => {

    const [musicChoice, setMusicChoice] = useState<string | undefined>("");
    const [musicEndpoint, setMusicEndpoint] = useState<string | undefined>("");

    useEffect(() => {
        console.log("ini", initValues)
        setMusicChoice(initValues?.musicConfig?.choice)
        setMusicEndpoint(initValues?.musicConfig?.endpoint)
    }, [initValues]);

    const handleMusicSourceChange = (value: string) => {
        setMusicChoice(value);
    }

    const handleEndpointChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setMusicEndpoint(e.target.value);
    }

    return (

        <Form form={formInstance} preserve initialValues={initValues} onFinish={handleSaveDevice} layout="vertical">
            <Form.Item name="id" label="ID thiết bị" rules={[{required: true}]}>
                <Input disabled className="form-input" placeholder={"劫持后，请喊一遍小讯小讯，重新刷新页面，自动填充"}/>
            </Form.Item>
            <Form.Item name="name" label="Tên thiết bị" rules={[{required: true}]}>
                <Input className="form-input"/>
            </Form.Item>

            <Collapse defaultActiveKey={["1"]} className="form-input" destroyInactivePanel={false}>
                <Panel header="Cấu hình AI" key="1" forceRender>
                    <Form.Item name={["aiConfig", "choice"]} label="Chọn AI">
                        <Select className="form-input">
                            {r1Resources.aiList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "key"]} label="AI Key">
                        <Input className="form-input"/>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "systemPrompt"]} label="Prompt hệ thống AI">
                        <Input className="form-input" placeholder={"Bạn là một loa thông minh"}/>
                    </Form.Item>
                    <Form.Item name={["aiConfig", "chatHistoryNum"]} label="Số lượng tin nhắn lịch sử">
                        <InputNumber className="form-input"/>
                    </Form.Item>
                </Panel>

                <Panel header="Cấu hình HASS" key="2" forceRender>
                    <Form.Item name={["hassConfig", "endpoint"]} label="Địa chỉ HASS">
                        <Input className="form-input"/>
                    </Form.Item>
                    <Form.Item name={["hassConfig", "token"]} label="HASS Token">
                        <Input className="form-input"/>
                    </Form.Item>
                </Panel>

                <Panel header="Cấu hình nhạc" key="3" forceRender>
                    <Form.Item name={["musicConfig", "choice"]} label="Chọn nguồn nhạc">
                        <Select className="form-input" onChange={(value) => handleMusicSourceChange(value)}>
                            {r1Resources.musicList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>

                    <Row> <Col span={18}>
                        {
                            musicChoice !== "gequbao" &&
                            <Form.Item name={["musicConfig", "endpoint"]} label="API nguồn nhạc">
                                <Input className="form-input" onChange={(value) => handleEndpointChange(value)}/>
                            </Form.Item>
                        }
                    </Col>
                        <Col>
                            {
                                musicChoice === "NetEaseMusic" && <>
                                    <Button style={{"marginTop": "30px", "paddingRight": "15px"}} type={"link"}
                                            href={`${musicEndpoint}/qrlogin.html`} target={"_blank"}>Đăng nhập bằng mã QR</Button>
                                </>
                            }

                        </Col>
                    </Row>
                </Panel>

                <Panel header="Sách nói" key="4" forceRender>
                    <Form.Item name={["audioConfig", "choice"]} label="Chọn nguồn âm thanh">
                        <Select className="form-input">
                            {r1Resources.audioList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                </Panel>

                <Panel header="Cấu hình thời tiết" key="5" forceRender>
                    <Form.Item name={["weatherConfig", "choice"]} label="Nguồn thời tiết">
                        <Select className="form-input">
                            {r1Resources.weatherList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "endpoint"]} label="Địa chỉ API thời tiết">
                        <Input className="form-input" placeholder={"Địa chỉ API khác nhau tùy người dùng"}/>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "key"]} label="API KEY thời tiết">
                        <Input className="form-input" placeholder={"Đây không phải là khóa riêng tư"}/>
                    </Form.Item>
                    <Form.Item name={["weatherConfig", "locationId"]} label="Thành phố mặc định">
                        <Select
                            showSearch
                            placeholder="Vui lòng chọn thành phố"
                            optionFilterProp="children"
                            filterOption={(input: string, option?: { children: string }) =>
                                (option?.children ?? '').toLowerCase().includes(input.toLowerCase())
                            }
                        >
                            {r1Resources.cityLocations.map(city => (
                                <Select.Option
                                    key={city.locationId}
                                    value={city.locationId}
                                >
                                    {city.cityName}
                                </Select.Option>
                            ))}
                        </Select>
                    </Form.Item>
                </Panel>

                <Panel header="Cấu hình tin tức" key="6" forceRender>
                    <Form.Item name={["newsConfig", "choice"]} label="Chọn nguồn tin tức">
                        <Select className="form-input">
                            {r1Resources.newsList.map(item => {
                                return <Option key={item.serviceName} value={item.serviceName}>{item.aliasName}</Option>
                            })}
                        </Select>
                    </Form.Item>
                </Panel>
            </Collapse>

            <Form.Item>
                <Button type="primary" htmlType="submit" icon={<SaveOutlined/>} className="button-save">
                    Lưu cấu hình
                </Button>
            </Form.Item>
        </Form>
    )
}

export default DeviceForm;
