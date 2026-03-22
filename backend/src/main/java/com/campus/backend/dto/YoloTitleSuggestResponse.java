package com.campus.backend.dto;

/**
 * YOLO 识别标题/描述建议响应 DTO。
 *
 * <p>后端会将该 DTO 返回给前端，前端用来自动填充“卖出宝贝”弹窗中的标题和描述。</p>
 */
public class YoloTitleSuggestResponse {

  /** 建议的标题，例如 “iPhone（含充电器+包装盒）” */
  private String titleSuggest;

  /** 建议的描述模板，可选字段，前端在描述为空时可用作初始文案。 */
  private String descTemplate;

  public String getTitleSuggest() {
    return titleSuggest;
  }

  public void setTitleSuggest(String titleSuggest) {
    this.titleSuggest = titleSuggest;
  }

  public String getDescTemplate() {
    return descTemplate;
  }

  public void setDescTemplate(String descTemplate) {
    this.descTemplate = descTemplate;
  }
}

