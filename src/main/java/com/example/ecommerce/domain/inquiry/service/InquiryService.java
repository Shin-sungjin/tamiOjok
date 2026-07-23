package com.example.ecommerce.domain.inquiry.service;

import com.example.ecommerce.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.example.ecommerce.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.ecommerce.domain.inquiry.dto.response.InquiryResponse;
import com.example.ecommerce.domain.inquiry.entity.Inquiry;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.inquiry.repository.InquiryRepository;
import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.order.service.OrderService;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.repository.UserRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Transactional
    public InquiryResponse createInquiry(Long userId, InquiryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Order order = null;
        if (request.orderId() != null) {
            order = orderService.getOrderEntityOrThrow(request.orderId());
            if (!order.isOwnedBy(userId)) {
                throw new CustomException(ErrorCode.ORDER_ACCESS_DENIED);
            }
        }

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .order(order)
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .build();

        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }

    public InquiryResponse getMyInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        validateOwnership(inquiry, userId);
        return InquiryResponse.from(inquiry);
    }

    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return inquiryRepository.findByUser(user, pageable).map(InquiryResponse::from);
    }

    public Page<InquiryResponse> getInquiries(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiries = status != null
                ? inquiryRepository.findByStatus(status, pageable)
                : inquiryRepository.findAll(pageable);
        return inquiries.map(InquiryResponse::from);
    }

    @Transactional
    public InquiryResponse answerInquiry(Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = getInquiryOrThrow(inquiryId);
        inquiry.answer(request.answer());
        return InquiryResponse.from(inquiry);
    }

    private Inquiry getInquiryOrThrow(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private void validateOwnership(Inquiry inquiry, Long userId) {
        if (!inquiry.isOwnedBy(userId)) {
            throw new CustomException(ErrorCode.INQUIRY_ACCESS_DENIED);
        }
    }
}
