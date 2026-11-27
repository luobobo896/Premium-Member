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
}

export async function getMembersApi(params: MemberApi.MemberQuery) {
  return requestClient.get<MemberApi.MemberPage>('/members', { params });
}
