package com.zsj.modules.ums.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用选项DTO（用于下拉框：id + name）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionDTO {
    private Long id;
    private String name;
}
