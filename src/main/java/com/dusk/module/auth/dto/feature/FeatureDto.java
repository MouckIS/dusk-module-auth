package com.dusk.module.auth.dto.feature;

import com.dusk.module.auth.dto.TenantFeature;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author kefuming
 * @version 0.0.1
 * @date 2020/5/9 10:01
 */
@Data
@ToString
public class FeatureDto extends TenantFeature
{
    private String featureValue;
    private List<FeatureDto> children;

    public FeatureDto(String name, String defaultValue, String displayName, String description){
        this.setName(name);
        this.setDefaultValue(defaultValue);
        this.setDisplayName(displayName);
        this.setDescription(description);
    }

    public FeatureDto CreateChild(String name, String defaultValue, String displayName, String description){
        FeatureDto item = new FeatureDto(name, defaultValue, displayName, description);
        item.setParentName(this.getName());
        this.getChildren().add(item);
        return item;
    }

    public void addChild(FeatureDto featureDto){
        this.getChildren().add(featureDto);
    }

    public void RemoveChildFeature(String name){
        this.children.forEach(item ->{
            if(item.getName().equals(name)){
                this.children.remove(item);
            }
        });
    }
}