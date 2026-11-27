import { requestClient } from '#/api/request';

export namespace BenefitApi {
  export interface Product {
    createdAt: string;
    description: string;
    id: number;
    lowStock: boolean;
    name: string;
    rules: string;
    skuCode: string;
    sort: number;
    status: number;
    stock: number;
    updatedAt: string;
  }

  export interface ProductPayload {
    description?: string;
    name: string;
    rules?: string;
    skuCode: string;
    sort?: number;
    status?: number;
  }

  export interface Card {
    batchNo: string;
    claimedAt: string | null;
    claimedBy: number | null;
    codeMasked: string;
    createdAt: string;
    expireAt: string | null;
    id: number;
    productSku: string;
    status: string;
  }

  export interface CardQuery {
    batchNo?: string;
    page: number;
    pageSize: number;
    productSku?: string;
    status?: string;
  }

  export interface CardImportPayload {
    batchNo: string;
    codes: string[];
    expireAt?: string;
    productSku: string;
  }

  export interface Claim {
    cardMasked: string | null;
    claimedAt: string | null;
    createdAt: string;
    id: number;
    memberId: number;
    memberName: string;
    memberNo: string;
    periodId: number;
    periodNo: string;
    productName: string;
    productSku: string;
    remark: string;
    status: string;
  }

  export interface ClaimQuery {
    memberId?: number;
    page: number;
    pageSize: number;
    productSku?: string;
    status?: string;
  }

  export interface Page<T> {
    items: T[];
    total: number;
  }
}

export function getBenefitProductsApi() {
  return requestClient.get<BenefitApi.Product[]>('/system/benefit/products');
}

export function createBenefitProductApi(data: BenefitApi.ProductPayload) {
  return requestClient.post<BenefitApi.Product>('/system/benefit/products', data);
}

export function updateBenefitProductApi(
  id: number,
  data: BenefitApi.ProductPayload,
) {
  return requestClient.put<BenefitApi.Product>(
    `/system/benefit/products/${id}`,
    data,
  );
}

export function deleteBenefitProductApi(id: number) {
  return requestClient.delete(`/system/benefit/products/${id}`);
}

export function getBenefitCardsApi(params: BenefitApi.CardQuery) {
  return requestClient.get<BenefitApi.Page<BenefitApi.Card>>(
    '/system/benefit/cards',
    { params },
  );
}

export function importBenefitCardsApi(data: BenefitApi.CardImportPayload) {
  return requestClient.post<{ duplicated: number; imported: number }>(
    '/system/benefit/cards/import',
    data,
  );
}

export function getBenefitClaimsApi(params: BenefitApi.ClaimQuery) {
  return requestClient.get<BenefitApi.Page<BenefitApi.Claim>>(
    '/system/benefit/claims',
    { params },
  );
}

export function reviewBenefitClaimApi(
  id: number,
  data: { reason: string; result: 'COMPENSATED' | 'FAILED' | 'SUCCESS' },
) {
  return requestClient.post(`/system/benefit/claims/${id}/review`, data);
}
