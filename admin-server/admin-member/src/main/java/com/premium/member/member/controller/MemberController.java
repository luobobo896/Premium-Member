package com.premium.member.member.controller;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.member.dto.MemberCreateRequest;
import com.premium.member.member.dto.MemberQuery;
import com.premium.member.member.dto.MemberResponse;
import com.premium.member.member.dto.MemberUpdateRequest;
import com.premium.member.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 分页查询会员。
     *
     * @param query page/pageSize/keyword/level/status
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('member:list')")
    public R<PageResult<MemberResponse>> list(@Valid MemberQuery query) {
        return R.ok(memberService.page(query));
    }

    /**
     * 查询会员详情。
     *
     * @param id 会员 ID
     * @return 会员
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:read')")
    public R<MemberResponse> detail(@PathVariable long id) {
        return R.ok(memberService.detail(id));
    }

    /**
     * 创建会员。
     *
     * @param request 创建参数
     * @return 新会员
     */
    @PostMapping
    @PreAuthorize("hasAuthority('member:create')")
    public R<MemberResponse> create(@Valid @RequestBody MemberCreateRequest request) {
        return R.ok(memberService.create(request));
    }

    /**
     * 更新会员。
     *
     * @param id      会员 ID
     * @param request 更新参数
     * @return 更新后的会员
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:update')")
    public R<MemberResponse> update(@PathVariable long id, @Valid @RequestBody MemberUpdateRequest request) {
        return R.ok(memberService.update(id, request));
    }

    /**
     * 删除会员。
     *
     * @param id 会员 ID
     * @return 空数据响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('member:delete')")
    public R<Void> delete(@PathVariable long id) {
        memberService.delete(id);
        return R.ok();
    }
}
