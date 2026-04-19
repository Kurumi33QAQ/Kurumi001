export interface Product {
  id: string | number;
  name: string;
  subTitle?: string;
  price: number;
  stock: number;
  sale: number;
  pic?: string;
  publishStatus: number;
  deleteStatus: number;
  createTime?: string;
  updateTime?: string;
}

export interface ProductQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  publishStatus?: number;
  minPrice?: number;
  maxPrice?: number;
}

export interface ProductPayload {
  id?: string | number;
  name: string;
  subTitle?: string;
  price: number;
  stock: number;
  pic?: string;
}
