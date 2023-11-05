package com.flyerssoft.ams.model.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Custom page dto class .
 *
 * @param <T> data
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomPageDto<T> {
  private T content;
  private long totalElements;
  private int totalPages;
  private int offset;
  private int pageNo;
  private int numberOfElements;
}
