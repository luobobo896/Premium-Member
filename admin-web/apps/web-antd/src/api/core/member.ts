import { requestClient } from '#/api/request';

export namespace MemberApi {
  export interface Member {
    balance: number;
    id: number;
    joinedAt: string;
    level: string;
    memberNo: string;
    nickname: string;
    phone: string;
    points: number;
    remark: string;
    status: number;
    username: string;
  }

  export interface MemberPage {
    items: Member[];
    total: number;
  }

  export interface MemberQuery {
    page: number;
    pageSize: number;
  }

  export interface MemberPayload {
    balance?: number;
    joinedAt?: string;
    level?: string;
    nickname?: string;
    phone?: string;
    points?: number;
    remark?: string;
    status?: number;
    username: string;
  }
}

export async function getMembersApi(params: MemberApi.MemberQuery) {
  return requestClient.get<MemberApi.MemberPage>('/members', { params });
}

export async function getMemberApi(id: number) {
  return requestClient.get<MemberApi.Member>(`/members/${id}`);
}

export async function createMemberApi(data: MemberApi.MemberPayload) {
  return requestClient.post<MemberApi.Member>('/members', data);
}

export async function updateMemberApi(
  id: number,
  data: MemberApi.MemberPayload,
) {
  return requestClient.put<MemberApi.Member>(`/members/${id}`, data);
}

export async function deleteMemberApi(id: number) {
  return requestClient.delete(`/members/${id}`);
}
