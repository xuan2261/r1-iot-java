package huan.diy.r1iot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceAliasName {
    private String serviceName;
    private String aliasName;
}
