const MEMBER_ACCESS_TOKEN_KEY = "mymall_member_access_token";

export function getMemberAccessToken() {
  return localStorage.getItem(MEMBER_ACCESS_TOKEN_KEY) || "";
}

export function setMemberAccessToken(token: string) {
  localStorage.setItem(MEMBER_ACCESS_TOKEN_KEY, token);
}

export function clearMemberAccessToken() {
  localStorage.removeItem(MEMBER_ACCESS_TOKEN_KEY);
}
