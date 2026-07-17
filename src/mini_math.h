#pragma once

#include <cmath>

// ============================================================
//  Minimal math library: just enough vec3 / mat4 for this game.
//  Avoids needing GLM as an extra dependency to fetch.
// ============================================================

struct Vec3
{
    float x = 0, y = 0, z = 0;

    Vec3() = default;
    Vec3(float x_, float y_, float z_) : x(x_), y(y_), z(z_) {}

    Vec3 operator+(const Vec3& o) const { return Vec3(x + o.x, y + o.y, z + o.z); }
    Vec3 operator-(const Vec3& o) const { return Vec3(x - o.x, y - o.y, z - o.z); }
    Vec3 operator*(float s) const { return Vec3(x * s, y * s, z * s); }

    static float dot(const Vec3& a, const Vec3& b)
    {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    static Vec3 cross(const Vec3& a, const Vec3& b)
    {
        return Vec3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        );
    }

    static Vec3 normalize(const Vec3& v)
    {
        float len = std::sqrt(dot(v, v));
        if (len < 1e-6f) return Vec3(0, 0, 0);
        return Vec3(v.x / len, v.y / len, v.z / len);
    }
};

// Column-major 4x4 matrix, matching OpenGL's expected layout.
struct Mat4
{
    float m[16];

    Mat4()
    {
        identity();
    }

    void identity()
    {
        for (int i = 0; i < 16; i++) m[i] = 0.0f;
        m[0] = m[5] = m[10] = m[15] = 1.0f;
    }

    static Mat4 translate(const Vec3& t)
    {
        Mat4 result;
        result.m[12] = t.x;
        result.m[13] = t.y;
        result.m[14] = t.z;
        return result;
    }

    static Mat4 scale(const Vec3& s)
    {
        Mat4 result;
        result.m[0] = s.x;
        result.m[5] = s.y;
        result.m[10] = s.z;
        return result;
    }

    static Mat4 rotateY(float radians)
    {
        Mat4 result;
        float c = std::cos(radians);
        float s = std::sin(radians);
        result.m[0] = c;
        result.m[2] = -s;
        result.m[8] = s;
        result.m[10] = c;
        return result;
    }

    static Mat4 rotateX(float radians)
    {
        Mat4 result;
        float c = std::cos(radians);
        float s = std::sin(radians);
        result.m[5] = c;
        result.m[6] = s;
        result.m[9] = -s;
        result.m[10] = c;
        return result;
    }

    static Mat4 multiply(const Mat4& a, const Mat4& b)
    {
        Mat4 result;
        for (int col = 0; col < 4; col++)
        {
            for (int row = 0; row < 4; row++)
            {
                float sum = 0.0f;
                for (int k = 0; k < 4; k++)
                {
                    sum += a.m[k * 4 + row] * b.m[col * 4 + k];
                }
                result.m[col * 4 + row] = sum;
            }
        }
        return result;
    }

    static Mat4 perspective(float fovYRadians, float aspect, float nearZ, float farZ)
    {
        Mat4 result;
        for (int i = 0; i < 16; i++) result.m[i] = 0.0f;

        float tanHalfFovy = std::tan(fovYRadians / 2.0f);

        result.m[0] = 1.0f / (aspect * tanHalfFovy);
        result.m[5] = 1.0f / tanHalfFovy;
        result.m[10] = -(farZ + nearZ) / (farZ - nearZ);
        result.m[11] = -1.0f;
        result.m[14] = -(2.0f * farZ * nearZ) / (farZ - nearZ);
        result.m[15] = 0.0f;

        return result;
    }

    static Mat4 lookAt(const Vec3& eye, const Vec3& center, const Vec3& up)
    {
        Vec3 f = Vec3::normalize(center - eye);
        Vec3 s = Vec3::normalize(Vec3::cross(f, up));
        Vec3 u = Vec3::cross(s, f);

        Mat4 result;
        result.m[0] = s.x;  result.m[4] = s.y;  result.m[8]  = s.z;
        result.m[1] = u.x;  result.m[5] = u.y;  result.m[9]  = u.z;
        result.m[2] = -f.x; result.m[6] = -f.y; result.m[10] = -f.z;

        result.m[12] = -Vec3::dot(s, eye);
        result.m[13] = -Vec3::dot(u, eye);
        result.m[14] = Vec3::dot(f, eye);
        result.m[15] = 1.0f;

        return result;
    }

    // Upper-left 3x3, used for transforming normals (assumes uniform scale
    // for simplicity in this prototype).
    void toNormalMatrix3(float out[9]) const
    {
        out[0] = m[0]; out[1] = m[1]; out[2] = m[2];
        out[3] = m[4]; out[4] = m[5]; out[5] = m[6];
        out[6] = m[8]; out[7] = m[9]; out[8] = m[10];
    }
};
