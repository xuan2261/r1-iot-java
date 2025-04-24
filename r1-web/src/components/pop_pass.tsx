import React, {useState, useEffect} from 'react';
import {Button, Input, Modal} from 'antd';
import type {InputRef} from 'antd';
import axiosInstance from "./api";

const PasswordModal = ({onClose}: { onClose: () => void }) => {
    const [password, setPassword] = useState('');
    const inputRef = React.useRef<InputRef>(null);

    // Focus vào ô nhập liệu
    useEffect(() => {
        inputRef.current?.focus();
    }, []);
    const apiURL = process.env.REACT_APP_API_URL;

    // Xử lý khi gửi
    const handleSubmit = async () => {
        if (!password) {
            Modal.warning({
                title: 'Thông báo',
                content: 'Vui lòng nhập mật khẩu',
            });
            return;
        }


        try {
            const response = await axiosInstance.post(`${apiURL}auth`, {password});
            if (response.status === 200) {
                window.localStorage.setItem("token", response.data);
                Modal.success({
                    title: 'Thành công',
                    content: 'Xác thực mật khẩu thành công',
                });
                // Làm mới trang hiện tại
                window.location.reload();
            }
        } catch (err) {
            console.error('Xác thực thất bại:', err);

            Modal.error({
                title: 'Lỗi',
                content: 'Xác thực mật khẩu thất bại',
            });
        }

        onClose();

    };

    // Xử lý sự kiện bàn phím
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSubmit();
        }
    };

    return (
        <div style={{
            position: 'fixed',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            zIndex: 1000,
            width: '300px'
        }}>
            <h3 style={{marginBottom: '16px'}}>Vui lòng nhập mật khẩu</h3>
            <Input.Password
                ref={inputRef}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Tham số password trong biến môi trường khi khởi động hệ thống"
                style={{width: '100%', marginBottom: '16px'}}
                onKeyDown={handleKeyDown}
            />
            <div style={{display: 'flex', justifyContent: 'flex-end'}}>
                <Button
                    onClick={onClose}
                    style={{marginRight: '10px'}}
                >
                    Hủy
                </Button>
                <Button
                    type="primary"
                    onClick={handleSubmit}
                    loading={false} // Có thể đặt thành true để hiển thị trạng thái đang tải
                >
                    Gửi
                </Button>
            </div>
        </div>
    );
};

export default PasswordModal;