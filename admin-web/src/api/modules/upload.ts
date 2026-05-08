import { request } from "@/api/http";

const IMAGE_UPLOAD_API = (import.meta.env.VITE_IMAGE_UPLOAD_API || "").trim();
const OSS_PUBLIC_BASE_URL = (import.meta.env.VITE_OSS_PUBLIC_BASE_URL || "").trim();

type UploadResponse =
  | string
  | {
      url?: string;
      fileUrl?: string;
      objectUrl?: string;
      key?: string;
      objectKey?: string;
      path?: string;
    };

function joinPublicBase(base: string, key: string) {
  if (!base) return key;
  return `${base.replace(/\/$/, "")}/${key.replace(/^\//, "")}`;
}

export function hasImageUploadApi() {
  return Boolean(IMAGE_UPLOAD_API);
}

export async function uploadImageApi(file: File) {
  if (!IMAGE_UPLOAD_API) {
    throw new Error("未配置图片上传接口");
  }

  const formData = new FormData();
  formData.append("file", file);

  const res = await request<UploadResponse>({
    url: IMAGE_UPLOAD_API,
    method: "post",
    data: formData,
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });

  if (typeof res === "string" && res) {
    return res;
  }

  const payload = typeof res === "string" ? undefined : res;
  const url = payload?.url || payload?.fileUrl || payload?.objectUrl;
  if (url) {
    return url;
  }

  const key = payload?.key || payload?.objectKey || payload?.path;
  if (key) {
    return joinPublicBase(OSS_PUBLIC_BASE_URL, key);
  }

  throw new Error("上传接口未返回可用图片地址");
}


